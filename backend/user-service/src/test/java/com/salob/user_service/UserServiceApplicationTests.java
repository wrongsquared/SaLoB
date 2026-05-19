package com.salob.user_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class UserServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
