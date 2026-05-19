package com.salob.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
    "REDIS_HOST=localhost",
    "REDIS_PORT=6379",
    "REDIS_PASSWORD=",
    "JWK_SET_URI=http://localhost:9999/.well-known/jwks.json",
    "FOOD_SERVICE_URI=http://localhost:9999",
    "USER_SERVICE_URI=http://localhost:9999",
    "APP_PORT=0",
    "spring.main.allow-bean-definition-overriding=true"
})
@Import(TestcontainersConfiguration.class)
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
