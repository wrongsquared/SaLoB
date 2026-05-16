package com.salob.user_service.api.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.salob.user_service.api._domain.Role;
import com.salob.user_service.api._domain.User;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class JwtService {
    @Value("${jwt.expiration-seconds}")
    private long expirationSeconds;

    @Getter
    private final RSAKey rsaKey;

    public JwtService(
            @Value("${jwt.keystore-password}") String password,
            @Value("${jwt.key-alias}") String alias
    ) throws Exception {

        // Load the Keystore from classpath
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("salob.p12")) {
            if (is == null) throw new FileNotFoundException("Keystore file salob.p12 not found!");
            keyStore.load(is, password.toCharArray());
        }

        // Extract Public and Private Key components
        RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(alias, password.toCharArray());
        Certificate cert = keyStore.getCertificate(alias);
        RSAPublicKey publicKey = (RSAPublicKey) cert.getPublicKey();

        // Build the immutable Nimbus JWK object matching your Key ID
        this.rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(alias)
                .build();
    }

    public Optional<String> createJwt(User user) {
        try {
            List<String> roleLabels = user.getRoles().stream()
                    .map(Role::getLabel)
                    .sorted()
                    .toList();

            // Using Auth0 java-jwt matching your current code structure
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) rsaKey.toPublicKey(), (RSAPrivateKey) rsaKey.toPrivateKey());
            String jwt = JWT.create()
                    .withKeyId(rsaKey.getKeyID()) // Embed kid in the header
                    .withSubject(user.getId().toString())
                    .withClaim("username", user.getUsername())
                    .withArrayClaim("roles", roleLabels.toArray(new String[0]))
                    .withExpiresAt(new Date(System.currentTimeMillis() + expirationSeconds * 1000))
                    .sign(algorithm);

            return Optional.of(jwt);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Map<String, Object> getJwksJson() {
        return new JWKSet(this.rsaKey).toJSONObject();
    }
}
