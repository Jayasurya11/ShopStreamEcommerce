package com.acme.shop.common;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@AutoConfiguration
@EnableScheduling
@Import({Outbox.class, OutboxRelay.class, ProcessedEvents.class})
public class CommonAutoConfiguration {

    @Bean
    KafkaAdmin.NewTopics shopTopics() {
        var topics = java.util.Arrays.stream(Topics.ALL)
            .map(t -> TopicBuilder.name(t).partitions(3).replicas(1).build())
            .toArray(org.apache.kafka.clients.admin.NewTopic[]::new);
        return new KafkaAdmin.NewTopics(topics);
    }

    /** Retry with exponential back-off (4 attempts), then park the poison message on "<topic>.DLT" instead of blocking the partition. */
    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
            (record, ex) -> new TopicPartition(record.topic() + ".DLT", -1));
        var backOff = new ExponentialBackOffWithMaxRetries(4);   // 1s, 2s, 4s, 8s, then dead-letter
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
