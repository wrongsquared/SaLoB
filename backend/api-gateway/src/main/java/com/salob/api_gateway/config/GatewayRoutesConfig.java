package com.salob.api_gateway.config;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service-route", r -> r.path("/api/auth/**", "/api/users/**")
                        .uri("lb://user-service"))
                .route("food-service-route", r -> r.path("/api/eateries/**", "/api/foods/**")
                        .uri("lb://food-service"))
                .build();
    }
}
