package com.sunday.services.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * Inbox-pattern dedup row for the Razorpay webhook (/api/payments/verify): a unique
 * (provider, eventReference) claim taken before any state mutation or live Razorpay call, so a
 * redelivered webhook can never double-transition a payment or double-publish Kafka events.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "processed_webhook_events", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "event_reference"}))
public class ProcessedWebhookEvent {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "event_reference", nullable = false)
    private String eventReference;

    @CreationTimestamp
    private LocalDateTime processedAt;
}
