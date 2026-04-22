package com.salob.salob_backend;

import org.springframework.boot.SpringApplication;

public class TestSalobBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(SalobBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
