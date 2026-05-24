package com.salob.food_service.api.onemap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "onemap")
public class OneMapProperties {
	private String apiUrl;
	private String email;
	private String password;
}
