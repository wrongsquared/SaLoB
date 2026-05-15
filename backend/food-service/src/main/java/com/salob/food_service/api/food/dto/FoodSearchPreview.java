package com.salob.food_service.api.food.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodSearchPreview {
    UUID foodId;
    String foodName;
    String photoUrl;
}
