package com.acme.shop.inventory;

import com.acme.shop.common.Events.*;
import com.acme.shop.common.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryEventListener {
    private static final String GROUP = "inventory-service";
    private record Locked(UUID id, String name, BigDecimal price, int stock, int wanted) {}

    private final JdbcClient jdbc;
    private final Outbox outbox;
    private final ProcessedEvents processed;

    public InventoryEventListener(JdbcClient jdbc, Outbox outbox, ProcessedEvents processed) {
        this.jdbc = jdbc; this.outbox = outbox; this.processed = processed;
    }

    /** Reserve stock and price the order. Emits InventoryReserved or InventoryRejected. */
    @KafkaListener(topics = Topics.ORDER_CREATED, groupId = GROUP)
    @Transactional
    public void onOrderCreated(String payload) {
        var e = Json.read(payload, OrderCreated.class);
        if (!processed.firstTime(e.eventId())) return;

        // lock rows in a stable order (by id) so concurrent orders can't deadlock each other
        var items = e.items().stream().sorted(Comparator.comparing(Item::productId)).toList();
        var locked = new ArrayList<Locked>();
        String problem = null;
        for (var item : items) {
            var row = jdbc.sql("select id, name, price, stock from products where id = :id for update")
                .param("id", item.productId())
                .query((rs, n) -> new Locked(rs.getObject("id", UUID.class), rs.getString("name"), rs.getBigDecimal("price"), rs.getInt("stock"), item.quantity()))
                .optional();
            if (row.isEmpty()) { problem = "Unknown product"; break; }
            if (row.get().stock() < item.quantity()) { problem = "Not enough stock for " + row.get().name(); break; }
            locked.add(row.get());
        }
        if (problem != null) {
            outbox.publish(Topics.INVENTORY_REJECTED, e.orderId(), new InventoryRejected(UUID.randomUUID(), e.orderId(), problem));
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (var l : locked) {
            jdbc.sql("update products set stock = stock - :q where id = :id").param("q", l.wanted()).param("id", l.id()).update();
            jdbc.sql("insert into reservations(order_id, product_id, quantity) values (:o, :p, :q)")
                .param("o", e.orderId()).param("p", l.id()).param("q", l.wanted()).update();
            total = total.add(l.price().multiply(BigDecimal.valueOf(l.wanted())));
        }
        outbox.publish(Topics.INVENTORY_RESERVED, e.orderId(), new InventoryReserved(UUID.randomUUID(), e.orderId(), total));
    }

    /** Saga compensation: payment failed, so put the reserved stock back. */
    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = GROUP)
    @Transactional
    public void onPaymentFailed(String payload) {
        var e = Json.read(payload, PaymentFailed.class);
        if (!processed.firstTime(e.eventId())) return;
        jdbc.sql("""
                update products p set stock = p.stock + r.quantity
                from reservations r where r.order_id = :o and r.product_id = p.id and not r.released""")
            .param("o", e.orderId()).update();
        jdbc.sql("update reservations set released = true where order_id = :o").param("o", e.orderId()).update();
    }
}
