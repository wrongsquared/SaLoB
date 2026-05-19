package com.salob.food_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration.
 *
 * WARNING: This allows all requests without authentication!
 * This is ONLY for development/testing. Never use in production.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())                    // Disable CSRF checks (for testing APIs)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()                // Allow all requests without authentication
                );
        return http.build();
    }
}
