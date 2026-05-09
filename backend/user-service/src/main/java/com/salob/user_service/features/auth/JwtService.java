package com.salob.user_service.features.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.salob.user_service.features.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.exp-seconds}")
    private int jwtExpSeconds;

    public Optional<String> issueJwt(User user) {
        try {
            String jwt = JWT.create()
                    .withSubject(user.getId().toString())
                    .withClaim("username", user.getUsername())
                    .withClaim("email", user.getEmail())
                    .withClaim("role", user.getRole().name())
                    .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpSeconds * 1000L))
                    .sign(getAlgorithm());
            return Optional.of(jwt);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Optional<DecodedJWT> decodeJwt(String jwt) {
        try {
            JWTVerifier verifier = JWT.require(getAlgorithm()).build();
            DecodedJWT decodedJWT = verifier.verify(jwt);
            return Optional.of(decodedJWT);
        } catch (Exception e) {
            log.warn("Failed to decode JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Algorithm getAlgorithm() {
        return Algorithm.HMAC256(jwtSecret);
    }
}
