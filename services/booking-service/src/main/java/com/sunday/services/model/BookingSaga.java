package com.sunday.services.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * Durable record of one createBooking saga attempt, keyed by the caller's Idempotency-Key (or a
 * server-generated one when absent). Written in its own REQUIRES_NEW transaction at every step
 * (see BookingSagaService) so a crash mid-createBooking still leaves an accurate trail for
 * BookingSagaRecoverySweeper to compensate from.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "booking_sagas", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "idempotency_key"}))
public class BookingSaga {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    private String bookingReference;

    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID bookingId;

    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    @Lob
    private String responseJson;

    @Column(length = 500)
    private String lastError;

    // Snapshot of the held seats, so the recovery sweeper can compensate even if no Booking row
    // was ever created (crash between seat hold and booking save).
    @ElementCollection
    @CollectionTable(name = "booking_saga_seat_instances", joinColumns = @JoinColumn(name = "booking_saga_id"))
    @Column(name = "seat_instance_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private List<UUID> seatInstanceIds;

    @Version
    private Long version;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
