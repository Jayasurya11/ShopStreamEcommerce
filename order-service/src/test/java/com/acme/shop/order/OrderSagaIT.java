package com.acme.shop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.shop.common.Events.InventoryReserved;
import com.acme.shop.common.Events.PaymentCompleted;
import com.acme.shop.common.Json;
import com.acme.shop.common.Topics;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Runs the order-service against real PostgreSQL and Kafka: outbox relay, idempotency and saga transitions. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderSagaIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");
    @Container static KafkaContainer kafka = new KafkaContainer("apache/kafka:3.8.0");

    @DynamicPropertySource
    static void containers(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcClient jdbc;
    @Autowired KafkaTemplate<String, String> kafkaTemplate;

    private static final String BODY = "{\"items\":[{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":2}]}";

    private String placeOrder(String email, String key) throws Exception {
        var res = mvc.perform(post("/api/v1/orders").header("X-User-Email", email).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.id");
    }

    private String statusOf(String orderId) {
        return jdbc.sql("select status from orders where id = :id").param("id", UUID.fromString(orderId)).query(String.class).single();
    }

    @Test
    void requestWithoutIdentityIsRejectedAsProblemDetail() throws Exception {
        mvc.perform(get("/api/v1/orders")).andExpect(status().isBadRequest());
    }

    @Test
    void sameIdempotencyKeyReturnsTheSameOrder() throws Exception {
        String key = UUID.randomUUID().toString();
        String first = placeOrder("idem@example.com", key);
        String second = placeOrder("idem@example.com", key);
        assertThat(second).isEqualTo(first);
        assertThat(jdbc.sql("select count(*) from orders where customer_email = 'idem@example.com'").query(Long.class).single()).isEqualTo(1L);
    }

    @Test
    void outboxEventIsRelayedToKafka() throws Exception {
        placeOrder("outbox@example.com", UUID.randomUUID().toString());
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
            assertThat(jdbc.sql("select count(*) from outbox_events where sent_at is null").query(Long.class).single()).isZero());
    }

    @Test
    void sagaEventsDriveTheOrderToConfirmedAndDuplicatesAreIgnored() throws Exception {
        String orderId = placeOrder("saga@example.com", UUID.randomUUID().toString());
        UUID id = UUID.fromString(orderId);

        kafkaTemplate.send(Topics.INVENTORY_RESERVED, orderId, Json.write(new InventoryReserved(UUID.randomUUID(), id, new BigDecimal("258.00"))));
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(statusOf(orderId)).isEqualTo("AWAITING_PAYMENT"));

        UUID paymentEventId = UUID.randomUUID();
        String paid = Json.write(new PaymentCompleted(paymentEventId, id));
        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, orderId, paid);
        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, orderId, paid);   // duplicate delivery (at-least-once)

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(statusOf(orderId)).isEqualTo("CONFIRMED"));
        assertThat(jdbc.sql("select count(*) from processed_events where event_id = :e").param("e", paymentEventId).query(Long.class).single()).isEqualTo(1L);
    }
}
