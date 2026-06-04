package com.salob.proto.events;

import java.util.UUID;

/**
 * Event published by food-service when a new food entry is submitted.
 * Consumers can listen to this event to trigger actions like updating user stats.
 */
public record FoodEntrySubmittedEvent(
        UUID eventId, // Idempotency
        // Used to update 'lastActivityAt' and submission count (+consequently, the 'WTF score')
        // for the submitter of this entry
        UUID submitterId
) {}
