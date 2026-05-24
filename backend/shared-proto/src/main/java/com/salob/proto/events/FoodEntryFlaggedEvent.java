package com.salob.proto.events;

/**
 * Event published by food-service when a food entry is flagged.
 * Consumers can listen to this event to trigger actions like updating user stats.
 */
public record FoodEntryFlaggedEvent(
    String eventId, // Idempotency
    String flaggerId, // Update 'lastActivityAt' for the flagger
    String flaggedEntrySubmitterId // Update the count of 'flagged entries' + 'WTF score' for the submitter of the flagged entry
) {}
