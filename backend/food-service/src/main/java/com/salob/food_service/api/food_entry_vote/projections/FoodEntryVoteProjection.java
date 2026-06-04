package com.salob.food_service.api.food_entry_vote.projections;

import java.util.UUID;

public record FoodEntryVoteProjection(UUID foodEntrySubmitterId, Boolean isUpvote) {
}
