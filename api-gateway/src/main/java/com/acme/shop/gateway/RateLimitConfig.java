package com.acme.shop.gateway;

import java.security.Principal;
import java.util.Optional;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    /** Rate-limit per authenticated user; fall back to the client IP for anonymous calls. Referenced from YAML as #{@userKeyResolver}. */
    @Bean
    KeyResolver userKeyResolver() {
        return exchange -> exchange.<Principal>getPrincipal()
            .filter(JwtAuthenticationToken.class::isInstance)
            .map(p -> "user:" + p.getName())
            .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(a -> a.getAddress().getHostAddress()).orElse("unknown")));
    }
}
