package com.acme.shop.common;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Polls unsent outbox rows (SKIP LOCKED, so several instances can run safely) and publishes them in order. */
@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private record Row(long id, String topic, String key, String payload) {}

    private final JdbcClient jdbc;
    private final KafkaTemplate<String, String> kafka;

    public OutboxRelay(JdbcClient jdbc, KafkaTemplate<String, String> kafka, MeterRegistry metrics) {
        this.jdbc = jdbc;
        this.kafka = kafka;
        // Backlog of events waiting to be published: alert on this staying above zero.
        Gauge.builder("shop.outbox.pending", () -> {
            try { return jdbc.sql("select count(*) from outbox_events where sent_at is null").query(Long.class).single(); }
            catch (Exception e) { return -1L; }
        }).description("Outbox events not yet published to Kafka").register(metrics);
    }

    /** Housekeeping: keep the outbox and inbox tables small. Retention must exceed the Kafka topic retention (7 days). */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    @Transactional
    public void cleanup() {
        int sent = jdbc.sql("delete from outbox_events where sent_at < now() - interval '7 days'").update();
        int seen = jdbc.sql("delete from processed_events where processed_at < now() - interval '30 days'").update();
        if (sent + seen > 0) log.info("Housekeeping removed {} outbox rows and {} processed-event rows", sent, seen);
    }

    @Scheduled(fixedDelayString = "${shop.outbox.poll-ms:300}")
    @Transactional
    public void relay() {
        var rows = jdbc.sql("""
                select id, topic, msg_key, payload from outbox_events
                where sent_at is null order by id limit 100 for update skip locked""")
            .query((rs, n) -> new Row(rs.getLong("id"), rs.getString("topic"), rs.getString("msg_key"), rs.getString("payload")))
            .list();
        for (var r : rows) {
            try {
                kafka.send(r.topic(), r.key(), r.payload()).get(10, TimeUnit.SECONDS);
                jdbc.sql("update outbox_events set sent_at = now() where id = :id").param("id", r.id()).update();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Outbox publish failed for row {}, will retry: {}", r.id(), e.toString());
                return; // stop here to preserve ordering
            }
        }
    }
}
