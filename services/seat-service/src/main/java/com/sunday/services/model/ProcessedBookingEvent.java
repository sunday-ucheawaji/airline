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
 * Inbox-pattern dedup row for the booking.confirmed/booking.cancelled consumers: a unique
 * (bookingId, eventType) claim taken before any seat mutation, so a Kafka redelivery can never
 * double-apply a side effect — a DB-enforced layer on top of the existing pessimistic-lock-based
 * state guards in SeatInstanceServiceImpl.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "processed_booking_events", uniqueConstraints = @UniqueConstraint(columnNames = {"booking_id", "event_type"}))
public class ProcessedBookingEvent {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID bookingId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @CreationTimestamp
    private LocalDateTime processedAt;
}
