package com.acme.shop.common;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional outbox: events are written in the SAME database transaction as the business change,
 * then relayed to Kafka by OutboxRelay. This avoids the "saved to DB but failed to publish" dual-write problem.
 */
@Component
public class Outbox {
    private final JdbcClient jdbc;
    public Outbox(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, UUID key, Object event) {
        jdbc.sql("insert into outbox_events(topic, msg_key, payload) values (:t, :k, :p)")
            .param("t", topic).param("k", key.toString()).param("p", Json.write(event)).update();
    }
}
