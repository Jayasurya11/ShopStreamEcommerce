package com.acme.shop.gateway;

import java.security.Principal;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Identity propagation. Any X-User-* header sent by the client is stripped (it could be forged), then
 * rebuilt from the VALIDATED token. Downstream services trust these headers, never the client.
 */
@Component
public class UserHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.<Principal>getPrincipal()
            .filter(JwtAuthenticationToken.class::isInstance)
            .cast(JwtAuthenticationToken.class)
            .map(t -> withIdentity(exchange, t.getToken().getSubject(), t.getToken().getClaimAsString("email")))
            .defaultIfEmpty(withIdentity(exchange, null, null))
            .flatMap(chain::filter);
    }

    private ServerWebExchange withIdentity(ServerWebExchange exchange, String id, String email) {
        var request = exchange.getRequest().mutate().headers(h -> {
            h.remove("X-User-Id");
            h.remove("X-User-Email");
            if (id != null) h.set("X-User-Id", id);
            if (email != null) h.set("X-User-Email", email);
        }).build();
        return exchange.mutate().request(request).build();
    }

    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE + 10; }
}
