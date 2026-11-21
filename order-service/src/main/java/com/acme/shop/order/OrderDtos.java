package com.acme.shop.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {
    private OrderDtos() {}
    public record ItemRequest(@NotNull UUID productId, @Min(1) @Max(10) int quantity) {}
    /** The customer is NOT part of the body: it comes from the validated token (X-User-Email, set by the gateway). */
    public record CreateOrderRequest(@NotEmpty @Size(max = 20) List<@Valid ItemRequest> items) {}
    public record OrderDto(UUID id, String status, BigDecimal total, String customerEmail, Instant createdAt, String failureReason) {}
    /** Pushed to browsers over SSE whenever an order changes state. */
    public record OrderUpdate(UUID orderId, String status, BigDecimal total, String failureReason) {}
}
