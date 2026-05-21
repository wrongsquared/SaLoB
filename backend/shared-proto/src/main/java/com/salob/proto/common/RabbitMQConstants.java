package com.salob.proto.common;

/**
 * Shared RabbitMQ routing constants used by both food-service (publisher)
 * and user-service (consumer). Eliminates magic string duplication across services.
 *
 * Convention:
 *   Exchange: salob.events (topic)
 *   Routing keys: {domain}.{action} — e.g., "wtf.vote.cast", "wtf.entry.created"
 *   Queues: {service}.{domain}.{purpose} — e.g., "user-service.wtf.recalc"
 */
public final class RabbitMQConstants {

    public static final String EVENTS_EXCHANGE = "salob.events";

    // Routing keys
    public static final String RK_WTF_ENTRY_CREATED = "wtf.entry.created";
    public static final String RK_WTF_VOTE_CAST = "wtf.vote.cast";
    public static final String RK_WTF_FLAG_RAISED = "wtf.flag.raised";

    // Queues
    public static final String QUEUE_WTF_RECALC = "user-service.wtf.recalc";

    private RabbitMQConstants() {}
}
