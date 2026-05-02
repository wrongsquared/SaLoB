package com.salob.food_service;

import org.springframework.boot.SpringApplication;

public class TestFoodServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(FoodServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
