package com.salob.api_gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {
//    @Bean
//    public KeyResolver ipKeyResolver() {
//        return exchange -> Mono.just(
//                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
//        );
//    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            return Mono.just(authHeader != null ? authHeader : "anonymous");
        };
    }
}