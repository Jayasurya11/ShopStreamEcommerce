package com.acme.shop.order;

import com.acme.shop.order.OrderDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService service;
    private final OrderEventsHub hub;

    public OrderController(OrderService service, OrderEventsHub hub) { this.service = service; this.hub = hub; }

    /** Send the same Idempotency-Key when retrying, so a network glitch can never create two orders. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderDto create(@RequestHeader("X-User-Email") String email,
                    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                    @Valid @RequestBody CreateOrderRequest request) {
        if (idempotencyKey != null && idempotencyKey.length() > 80) throw new IllegalArgumentException("Idempotency-Key is too long.");
        return service.create(email, idempotencyKey, request.items());
    }

    @GetMapping
    List<OrderDto> list(@RequestHeader("X-User-Email") String email) { return service.list(email); }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@RequestHeader("X-User-Email") String email) { return hub.subscribe(email); }
}
