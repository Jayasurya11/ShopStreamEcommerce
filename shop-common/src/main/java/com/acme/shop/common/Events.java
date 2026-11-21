package com.acme.shop.common;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** The event contract shared by all services. Every event carries a unique eventId for idempotent consumers. */
public final class Events {
    private Events() {}
    public record Item(UUID productId, int quantity) {}
    public record OrderCreated(UUID eventId, UUID orderId, List<Item> items) {}
    public record InventoryReserved(UUID eventId, UUID orderId, BigDecimal total) {}
    public record InventoryRejected(UUID eventId, UUID orderId, String reason) {}
    public record PaymentCompleted(UUID eventId, UUID orderId) {}
    public record PaymentFailed(UUID eventId, UUID orderId, String reason) {}
}
