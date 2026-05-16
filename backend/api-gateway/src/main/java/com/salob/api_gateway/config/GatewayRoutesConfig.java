package com.salob.api_gateway.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {
    @Value("${app.routing.user-service}")
    private String userServiceUri;

    @Value("${app.routing.food-service}")
    private String foodServiceUri;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service-route", r -> r
                        .path("/api/auth/**", "/api/users/**", "/.well-known/**")
                        .uri(userServiceUri))
                .route("food-service-route", r -> r
                        .path("/api/eateries/**", "/api/foods/**", "/api/food-entries/**")
                        .uri(foodServiceUri))
                .build();
    }
}
