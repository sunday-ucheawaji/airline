package com.sunday.services.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * One row per event pending (or attempted) delivery to Kafka. Written in the same transaction as
 * the business change it describes — see {@code OutboxEventPublisher} — so the two can never
 * disagree about whether the change actually happened.
 */
@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_events_status_created", columnList = "status, created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String topic;

    /** Fully-qualified class name, used by the sweeper to deserialize {@link #payload} back into
     *  the real event object before handing it to the Object-typed KafkaTemplate — this keeps the
     *  wire format (including the type header Spring Kafka's JsonSerializer embeds) identical to
     *  a direct send, so existing consumers don't need to change. */
    @Column(name = "event_type", nullable = false, length = 255)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;
}
