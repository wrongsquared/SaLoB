package com.salob.user_service.api._domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks already-processed WTF events for idempotent consumption.
 * Before processing an event, the consumer checks if its eventId exists here.
 * If yes, the event is skipped (RabbitMQ redelivery after crash).
 *
 * Rows are periodically cleaned up by a scheduled task (older than N days).
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
