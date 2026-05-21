package com.salob.food_service.api.food_entry.dto;

import java.time.Instant;

public record DatePrice(Instant date, Integer sgCents, Double confidence, Integer entryCount) {
}
