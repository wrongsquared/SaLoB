package com.salob.proto.events;

import java.util.UUID;

/**
 * Generic WTF event published by food-service and consumed by user-service.
 * All WTF-related events share this single record — the {@code type} field
 * tells the consumer which counter to increment and which handler to call.
 *
 * @param type           Event type discriminator: "ENTRY_SUBMITTED" | "VOTE_CAST" | "FLAG_RAISED"
 * @param eventId        Unique ID for idempotent processing (consumer stores this to skip duplicates)
 * @param actorId        The user who performed the action (used to update lastActivityAt)
 * @param targetUserId   The user whose stats are affected (counters to increment)
 * @param isUpvote       Only meaningful when type = "VOTE_CAST". Tells consumer to increment
 *                       upvotesReceived vs downvotesReceived.
 */
public record WtfEvent(
        String type,
        UUID eventId,
        UUID actorId,
        UUID targetUserId,
        Boolean isUpvote
) {}
