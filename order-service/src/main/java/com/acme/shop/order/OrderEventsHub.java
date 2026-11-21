package com.acme.shop.order;

import com.acme.shop.common.Json;
import com.acme.shop.order.OrderDtos.OrderUpdate;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Fan-out of order state changes to connected browsers (SSE). Each user only receives their own orders. */
@Component
public class OrderEventsHub {
    private record Subscriber(String email, SseEmitter emitter) {}
    private final List<Subscriber> subscribers = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe(String email) {
        var sub = new Subscriber(email.toLowerCase(), new SseEmitter(0L));
        subscribers.add(sub);
        sub.emitter().onCompletion(() -> subscribers.remove(sub));
        sub.emitter().onTimeout(() -> subscribers.remove(sub));
        sub.emitter().onError(t -> subscribers.remove(sub));
        send(sub, SseEmitter.event().name("hello").data("connected"));
        return sub.emitter();
    }

    public void broadcast(String email, OrderUpdate update) {
        var event = SseEmitter.event().name("order").data(Json.write(update));
        subscribers.stream().filter(s -> s.email().equalsIgnoreCase(email)).forEach(s -> send(s, event));
    }

    @Scheduled(fixedRate = 15000)
    void heartbeat() { subscribers.forEach(s -> send(s, SseEmitter.event().comment("ping"))); }

    private void send(Subscriber sub, SseEmitter.SseEventBuilder event) {
        try { sub.emitter().send(event); } catch (IOException | IllegalStateException ex) { subscribers.remove(sub); }
    }
}
