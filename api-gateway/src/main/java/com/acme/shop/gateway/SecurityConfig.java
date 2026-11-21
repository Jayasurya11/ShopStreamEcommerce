package com.acme.shop.gateway;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

/** The gateway is the OAuth2 resource server: it validates every Keycloak access token before routing. */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)   // stateless bearer tokens, no cookies
            .authorizeExchange(a -> a
                .pathMatchers(HttpMethod.OPTIONS).permitAll()
                .pathMatchers("/fallback/**").permitAll()
                .pathMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()   // public catalogue
                .pathMatchers("/actuator/**").permitAll()   // served on the separate management port (9080), never exposed publicly
                .pathMatchers("/api/**").authenticated()
                .anyExchange().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(keycloakRoles())))
            .build();
    }

    /** Keycloak puts realm roles in the "realm_access.roles" claim; expose them as ROLE_<name>. */
    @SuppressWarnings("unchecked")
    private ReactiveJwtAuthenticationConverterAdapter keycloakRoles() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            Collection<String> roles = realmAccess == null ? List.of() : (Collection<String>) realmAccess.getOrDefault("roles", List.of());
            return roles.stream().<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
