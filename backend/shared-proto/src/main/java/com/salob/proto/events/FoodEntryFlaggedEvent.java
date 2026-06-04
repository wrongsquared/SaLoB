package com.salob.proto.events;

import java.util.UUID;

/**
 * Event published by food-service when a food entry is flagged.
 * Consumers can listen to this event to trigger actions like updating user stats.
 */
public record FoodEntryFlaggedEvent(
        UUID eventId, // Idempotency
        UUID flaggerId, // Update 'lastActivityAt' for the flagger
        UUID flaggedEntrySubmitterId// Update the count of 'flagged entries' + 'WTF score' for the submitter of the flagged entry
) {}
