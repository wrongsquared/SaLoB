package com.salob.api_gateway;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class TokenRelayFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication().getPrincipal())
                .cast(Jwt.class)
                .map(jwt -> {
                    // Extract data from claims payload
                    String userId = jwt.getSubject(); // Assuming subject is user ID
                    String username = jwt.getClaimAsString("username");
                    List<String> roles = jwt.getClaimAsStringList("roles");
                    String rolesStr = roles != null ? String.join(",", roles) : "";

                    // Mutate the request to add custom downstream headers
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Name", username)
                            .header("X-User-Roles", rolesStr)
                            .build();

                    return exchange.mutate().request(mutatedRequest).build();
                })
                .defaultIfEmpty(exchange) // If no JWT (like /api/auth/login), pass exchange as-is
                .flatMap(chain::filter);
    }
}