package com.salob.food_service.api.food_entry.dto;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotNull Boolean isUpvote
) {}
