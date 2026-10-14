package com.sunday.services.model;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.enums.TripType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A round trip or multi-city journey: one booking per flight (each with its own fare, seats, tickets, changes and
 * refunds), all paid with one payment whose id is the trip's.
 */
@Entity
@Table(name = "trips")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trip {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String tripReference;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Column(name = "airline_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID airlineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TripType tripType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    private String currency;

    private BigDecimal totalAmount;

    @Column(name = "payment_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID paymentId;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
