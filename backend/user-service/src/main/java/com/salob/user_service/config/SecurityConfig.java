package com.salob.user_service.config;

import com.salob.user_service.api.auth.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)                    // Disable CSRF checks (for testing APIs)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()                // Allow all requests without authentication
                )
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}));
        return http.build();
    }

    // Build the decoder directly using the local public key loaded from the .p12 file
    @Bean
    public JwtDecoder jwtDecoder(JwtService jwtService) throws Exception {
        var publicKey = (RSAPublicKey) jwtService.getRsaKey().toPublicKey();
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }          
}