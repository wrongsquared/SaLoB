package com.salob.api_gateway.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {
    @Value("${USER_SERVICE_URL}")
    private String userServiceUrl;

    @Value("${FOOD_SERVICE_URL}")
    private String foodServiceUrl;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service-route", r -> r.path("/api/users/**")
                        .uri(userServiceUrl))
                .route("food-service-route", r -> r.path("/api/eateries/**", "/**") // Added fallback just in case
                        .uri(foodServiceUrl))
                .build();
    }
}
