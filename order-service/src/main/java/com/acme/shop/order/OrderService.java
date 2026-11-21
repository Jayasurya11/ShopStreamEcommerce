package com.acme.shop.order;

import com.acme.shop.common.Events;
import com.acme.shop.common.Outbox;
import com.acme.shop.common.Topics;
import com.acme.shop.order.OrderDtos.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class OrderService {
    private final JdbcClient jdbc;
    private final Outbox outbox;
    private final OrderEventsHub hub;

    public OrderService(JdbcClient jdbc, Outbox outbox, OrderEventsHub hub) {
        this.jdbc = jdbc; this.outbox = outbox; this.hub = hub;
    }

    /** Saga step 1: persist the order as PENDING and emit OrderCreated, atomically (outbox). */
    @Transactional
    public OrderDto create(String email, String idempotencyKey, List<ItemRequest> items) {
        Map<UUID, Integer> merged = new LinkedHashMap<>();
        items.forEach(i -> merged.merge(i.productId(), i.quantity(), Integer::sum));
        if (merged.values().stream().anyMatch(q -> q > 10)) throw new IllegalArgumentException("You can order at most 10 units of one product.");

        UUID id = UUID.randomUUID();
        if (idempotencyKey != null) {
            // A retried request (same key) returns the original order instead of creating a duplicate.
            int fresh = jdbc.sql("insert into idempotency_keys(customer_email, idem_key, order_id) values (:e, :k, :o) on conflict do nothing")
                .param("e", email.toLowerCase()).param("k", idempotencyKey).param("o", id).update();
            if (fresh == 0) {
                UUID existing = jdbc.sql("select order_id from idempotency_keys where customer_email = :e and idem_key = :k")
                    .param("e", email.toLowerCase()).param("k", idempotencyKey).query(UUID.class).single();
                return get(existing);
            }
        }
        jdbc.sql("insert into orders(id, customer_email, status) values (:id, :email, 'PENDING')")
            .param("id", id).param("email", email.toLowerCase()).update();
        merged.forEach((product, qty) -> jdbc.sql("insert into order_items(order_id, product_id, quantity) values (:o, :p, :q)")
            .param("o", id).param("p", product).param("q", qty).update());
        outbox.publish(Topics.ORDER_CREATED, id, new Events.OrderCreated(UUID.randomUUID(), id,
            merged.entrySet().stream().map(e -> new Events.Item(e.getKey(), e.getValue())).toList()));
        return get(id);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> list(String email) {
        return jdbc.sql("select * from orders where customer_email = :e order by created_at desc limit 50")
            .param("e", email.toLowerCase()).query(this::map).list();
    }

    // ---- state transitions driven by events from other services ----

    @Transactional
    public void onInventoryReserved(UUID orderId, BigDecimal total) {
        // total always recorded; status only advances if we are still PENDING (events can interleave)
        update(orderId, "update orders set total = :t, status = case when status = 'PENDING' then 'AWAITING_PAYMENT' else status end, updated_at = now() where id = :id",
            Map.of("t", total));
    }
    @Transactional
    public void onPaymentCompleted(UUID orderId) {
        update(orderId, "update orders set status = 'CONFIRMED', updated_at = now() where id = :id", Map.of());
    }
    @Transactional
    public void onRejected(UUID orderId, String status, String reason) {
        update(orderId, "update orders set status = :s, failure_reason = :r, updated_at = now() where id = :id", Map.of("s", status, "r", reason));
    }

    private void update(UUID id, String sql, Map<String, Object> params) {
        var spec = jdbc.sql(sql).param("id", id);
        for (var e : params.entrySet()) spec = spec.param(e.getKey(), e.getValue());
        spec.update();
        var o = get(id);
        var update = new OrderUpdate(o.id(), o.status(), o.total(), o.failureReason());
        // push to browsers only after the DB transaction has really committed
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { hub.broadcast(o.customerEmail(), update); }
        });
    }

    private OrderDto get(UUID id) {
        return jdbc.sql("select * from orders where id = :id").param("id", id).query(this::map).single();
    }

    private OrderDto map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        return new OrderDto(rs.getObject("id", UUID.class), rs.getString("status"), rs.getBigDecimal("total"),
            rs.getString("customer_email"), rs.getObject("created_at", OffsetDateTime.class).toInstant(), rs.getString("failure_reason"));
    }
}
