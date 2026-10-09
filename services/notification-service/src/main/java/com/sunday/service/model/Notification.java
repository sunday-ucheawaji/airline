package com.sunday.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One row per (eventType, reference, channel) — doubles as the delivery-tracking record AND the
 * idempotency claim for that send. "reference" is the natural key carried by the triggering event
 * (a bookingReference for booking events, the payment id for payment events), not a synthetic one.
 * The rendered subject/body is snapshotted here so a forced resend doesn't need to re-fetch or
 * re-render anything — same "snapshot at send time" pattern as payment-service's
 * Payment.initiateResponseJson.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(columnNames = {"event_type", "reference", "channel"}))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private NotificationEventType eventType;

    @Column(nullable = false, length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationStatus status;

    private String subject;

    @Lob
    private String body;

    @Column(name = "provider_message_id")
    private String providerMessageId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime sentAt;
}
