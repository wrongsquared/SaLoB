package com.salob.food_service.features.eatery.dto;
import java.time.LocalDate;

/**
 * For the data required by the frontend, each discrete point on
 * the graph for some food item (over time) represents a 'day'
 *
 * And each 'day' can have multiple 'price points' - thus we use
 * LocalDate so they can be grouped by date
 */
public record PricePoint(
        int sgCents,
        LocalDate date
) {}
