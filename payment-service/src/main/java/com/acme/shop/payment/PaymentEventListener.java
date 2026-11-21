package com.acme.shop.payment;

import com.acme.shop.common.Events.*;
import com.acme.shop.common.*;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventListener {
    private final JdbcClient jdbc;
    private final Outbox outbox;
    private final ProcessedEvents processed;
    private final PaymentProperties props;

    public PaymentEventListener(JdbcClient jdbc, Outbox outbox, ProcessedEvents processed, PaymentProperties props) {
        this.jdbc = jdbc; this.outbox = outbox; this.processed = processed; this.props = props;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED, groupId = "payment-service")
    @Transactional
    public void onReserved(String payload) throws InterruptedException {
        var e = Json.read(payload, InventoryReserved.class);
        if (!processed.firstTime(e.eventId())) return;

        Thread.sleep(props.getDelayMs()); // simulate the card network so the UI timeline is visible
        var decline = PaymentPolicy.declineReason(e.total(), props.getLimit());
        jdbc.sql("insert into payments(order_id, amount, status, reason) values (:o, :a, :s, :r)")
            .param("o", e.orderId()).param("a", e.total())
            .param("s", decline.isEmpty() ? "COMPLETED" : "FAILED").param("r", decline.orElse(null)).update();

        if (decline.isEmpty()) outbox.publish(Topics.PAYMENT_COMPLETED, e.orderId(), new PaymentCompleted(UUID.randomUUID(), e.orderId()));
        else outbox.publish(Topics.PAYMENT_FAILED, e.orderId(), new PaymentFailed(UUID.randomUUID(), e.orderId(), decline.get()));
    }
}
