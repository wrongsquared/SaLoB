package com.salob.food_service.seeding;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.seed.images")
public class SeedImageProperties {
    private boolean scrapeEnabled = true;
    private String imageSourceTemplate = "https://source.unsplash.com/featured/?{query}";
    private String userAgent = "SaLoBFoodSeeder/1.0";
    private int requestTimeoutSeconds = 10;
}
