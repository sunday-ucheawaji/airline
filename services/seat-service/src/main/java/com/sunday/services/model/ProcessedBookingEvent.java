package com.sunday.services.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @CreationTimestamp
    private LocalDateTime processedAt;
}
