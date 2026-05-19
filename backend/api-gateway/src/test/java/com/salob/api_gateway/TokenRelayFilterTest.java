package com.salob.api_gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * =============================================================================
 * WHAT THIS TEST TEACHES
 * =============================================================================
 *
 * This is a PURE UNIT test — no Spring context, no Testcontainers, no Docker.
 * The test verifies the filter's behavior in isolation by mocking its
 * dependencies (ServerWebExchange, GatewayFilterChain).
 *
 * Key technique: To make ReactiveSecurityContextHolder.getContext() return
 * a real authentication, we use .contextWrite(...) on the Mono pipeline.
 * This is the official Spring Security approach for testing reactive
 * security context propagation.
 *
 * The test uses StepVerifier from reactor-test (already a dependency)
 * to assert on reactive Mono<Void> pipelines.
 * =============================================================================
 */
@ExtendWith(MockitoExtension.class)
class TokenRelayFilterTest {

    @InjectMocks
    private TokenRelayFilter filter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpRequest.Builder requestBuilder;

    @Mock
    private ServerWebExchange.Builder exchangeBuilder;

    @Mock
    private GatewayFilterChain chain;

    @Test
    void filter_addsHeadersFromJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-abc-123")
                .claim("username", "johndoe")
                .claim("roles", List.of("CONTRIBUTOR"))
                .build();
        Authentication auth = new JwtAuthenticationToken(jwt);

        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header("X-User-Id", "user-abc-123")).thenReturn(requestBuilder);
        when(requestBuilder.header("X-User-Name", "johndoe")).thenReturn(requestBuilder);
        when(requestBuilder.header("X-User-Roles", "CONTRIBUTOR")).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(request)).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(
                filter.filter(exchange, chain)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        ).verifyComplete();

        verify(requestBuilder).header("X-User-Id", "user-abc-123");
        verify(requestBuilder).header("X-User-Name", "johndoe");
        verify(requestBuilder).header("X-User-Roles", "CONTRIBUTOR");
    }

    @Test
    void filter_handlesNullRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-xyz")
                .claim("username", "janedoe")
                .build();
        Authentication auth = new JwtAuthenticationToken(jwt);

        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header("X-User-Id", "user-xyz")).thenReturn(requestBuilder);
        when(requestBuilder.header("X-User-Name", "janedoe")).thenReturn(requestBuilder);
        when(requestBuilder.header("X-User-Roles", "")).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(request)).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(
                filter.filter(exchange, chain)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        ).verifyComplete();

        verify(requestBuilder).header("X-User-Roles", "");
    }

    @Test
    void filter_passesExchangeAsIsWhenNoAuthentication() {
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(
                filter.filter(exchange, chain)
        ).verifyComplete();
    }

    @Test
    void filter_rolesHeaderUsesCommaSeparatedValues() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-456")
                .claim("username", "bob")
                .claim("roles", List.of("ADMIN", "CONTRIBUTOR", "USER"))
                .build();
        Authentication auth = new JwtAuthenticationToken(jwt);

        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header("X-User-Id", "user-456")).thenReturn(requestBuilder);
        when(requestBuilder.header("X-User-Name", "bob")).thenReturn(requestBuilder);
        when(requestBuilder.header("X-User-Roles", "ADMIN,CONTRIBUTOR,USER")).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenReturn(request);

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(request)).thenReturn(exchangeBuilder);
        when(exchangeBuilder.build()).thenReturn(exchange);

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(
                filter.filter(exchange, chain)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
        ).verifyComplete();

        verify(requestBuilder).header("X-User-Roles", "ADMIN,CONTRIBUTOR,USER");
    }
}
