package com.salob.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
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

	@Value("${app.routing.ai-service}")
	private String aiServiceUri;

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder, RedisRateLimiter redisRateLimiter,
			KeyResolver ipKeyResolver) {
		return builder.routes()
				.route("user-service-route", r -> r.path("/api/auth/**", "/api/users/**", "/.well-known/**")
						.filters(f -> f.requestRateLimiter(
								config -> config.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
						.uri(userServiceUri))
				.route("food-service-route", r -> r.path("/api/eateries/**", "/api/foods/**", "/api/food-entries/**")
						.filters(f -> f.requestRateLimiter(
								config -> config.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
						.uri(foodServiceUri))
				.route("ai-service-route",
						r -> r.path("/api/chat/**").filters(f -> f.requestRateLimiter(
								config -> config.setRateLimiter(redisRateLimiter).setKeyResolver(ipKeyResolver)))
								.uri(aiServiceUri))
				.build();
	}
}
