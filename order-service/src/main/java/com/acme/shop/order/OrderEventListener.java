package com.acme.shop.order;

import com.acme.shop.common.Events.*;
import com.acme.shop.common.Json;
import com.acme.shop.common.ProcessedEvents;
import com.acme.shop.common.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Saga participant: reacts to events from inventory and payment services. */
@Component
public class OrderEventListener {
    private static final String GROUP = "order-service";
    private final OrderService orders;
    private final ProcessedEvents processed;

    public OrderEventListener(OrderService orders, ProcessedEvents processed) { this.orders = orders; this.processed = processed; }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED, groupId = GROUP)
    @Transactional
    public void onReserved(String payload) {
        var e = Json.read(payload, InventoryReserved.class);
        if (processed.firstTime(e.eventId())) orders.onInventoryReserved(e.orderId(), e.total());
    }

    @KafkaListener(topics = Topics.INVENTORY_REJECTED, groupId = GROUP)
    @Transactional
    public void onRejected(String payload) {
        var e = Json.read(payload, InventoryRejected.class);
        if (processed.firstTime(e.eventId())) orders.onRejected(e.orderId(), "REJECTED", e.reason());
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = GROUP)
    @Transactional
    public void onPaid(String payload) {
        var e = Json.read(payload, PaymentCompleted.class);
        if (processed.firstTime(e.eventId())) orders.onPaymentCompleted(e.orderId());
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = GROUP)
    @Transactional
    public void onPaymentFailed(String payload) {
        var e = Json.read(payload, PaymentFailed.class);
        if (processed.firstTime(e.eventId())) orders.onRejected(e.orderId(), "CANCELLED", e.reason());
    }
}
