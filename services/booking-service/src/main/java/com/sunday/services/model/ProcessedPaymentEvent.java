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
 * Inbox-pattern dedup row for PaymentEventListener: a unique (paymentId, eventType) claim taken
 * before any business mutation, so a Kafka redelivery can never double-apply a side effect even if
 * it races a concurrent delivery — a DB-enforced layer on top of the listener's own status checks.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "processed_payment_events", uniqueConstraints = @UniqueConstraint(columnNames = {"payment_id", "event_type"}))
public class ProcessedPaymentEvent {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "payment_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID paymentId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @CreationTimestamp
    private LocalDateTime processedAt;
}
