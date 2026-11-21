package com.acme.shop.common;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent consumer: Kafka delivers at-least-once, so each handler records the eventId it processed. */
@Component
public class ProcessedEvents {
    private final JdbcClient jdbc;
    public ProcessedEvents(JdbcClient jdbc) { this.jdbc = jdbc; }

    /** @return true the first time this event is seen, false for a duplicate delivery. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstTime(UUID eventId) {
        return jdbc.sql("insert into processed_events(event_id) values (:id) on conflict do nothing")
            .param("id", eventId).update() == 1;
    }
}
