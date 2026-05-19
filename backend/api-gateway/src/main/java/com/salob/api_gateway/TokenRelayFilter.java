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
                    String userId = jwt.getSubject();
                    String username = jwt.getClaimAsString("username");
                    List<String> roles = jwt.getClaimAsStringList("roles");
                    String rolesStr = roles != null ? String.join(",", roles) : "";

                    ServerHttpRequest.Builder builder = exchange.getRequest().mutate();

                    if (userId != null) {
                        builder.header("X-User-Id", userId);
                    }
                    if (username != null) {
                        builder.header("X-User-Name", username);
                    }

                    ServerHttpRequest mutatedRequest = builder
                            .header("X-User-Roles", rolesStr)
                            .build();

                    return exchange.mutate().request(mutatedRequest).build();
                })
                .defaultIfEmpty(exchange) // If no JWT (like /api/auth/login), pass exchange as-is
                .flatMap(chain::filter);
    }
}
