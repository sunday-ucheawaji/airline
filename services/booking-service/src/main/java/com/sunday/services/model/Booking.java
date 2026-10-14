package com.sunday.services.model;

import com.sunday.common_lib.embeddable.ContactInfo;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.TripType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "bookings")
public class Booking {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String bookingReference;

    @Column(name = "user_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Column(name = "flight_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID flightId;

    @Column(name = "flight_instance_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID flightInstanceId;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID airlineId;

    @Builder.Default
    private TripType tripType = TripType.ONE_WAY;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CabinClassType cabinClass = CabinClassType.ECONOMY;

    @Column(name = "fare_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID fareId;

    private Long fareVersion;
    private String fareCurrency;
    private BigDecimal fareBaseAmount;
    private BigDecimal fareTaxAmount;
    private BigDecimal fareFeeAmount;
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID fareRulesId;
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID fareBaggagePolicyId;
    private LocalDate fareTravelDate;
    private Boolean fareRefundable;
    private BigDecimal fareCancellationFee;
    private Integer fareRefundDeadlineDays;
    private Boolean fareChangeable;
    private BigDecimal fareChangeFee;
    private Integer fareChangeDeadlineHours;

    // The trip this flight belongs to; null for a one-way booking.
    @Column(name = "trip_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID tripId;

    @Column(name = "exchanged_from_booking_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID exchangedFromBookingId;

    @Column(name = "exchanged_to_booking_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID exchangedToBookingId;

    private BigDecimal totalAmount;

    private BigDecimal changeFeeAmount;

    private BigDecimal refundAmount;

    private boolean flexibleTicket;
    private LocalDateTime ticketTimeLimit;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Passenger> passengers = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "booking_seat_instances", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_instance_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private List<UUID> seatInstanceIds;

    @ElementCollection
    @CollectionTable(name = "booking_ancillaries", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "ancillary_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private List<UUID> ancillaryIds;

    @ElementCollection
    @CollectionTable(name = "booking_meals",
            joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "meal_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private List<UUID> mealIds;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Ticket> tickets = new HashSet<>();

    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @CreationTimestamp
    private LocalDateTime bookingDate;

    @UpdateTimestamp
    private LocalDateTime lastModified;

    private boolean ticketIssued;

    private ContactInfo contactInfo;

    @Version
    private Long version;

    /** What a refund can return: everything paid, except a change fee paid to create this booking. */
    public BigDecimal refundableValue() {
        if (totalAmount == null) {
            return BigDecimal.ZERO;
        }
        return totalAmount.subtract(changeFeeAmount != null ? changeFeeAmount : BigDecimal.ZERO);
    }

    /** The fee to change this booking: each passenger's own fare's change fee (the booking's, if priced before that). */
    public BigDecimal changeFees() {
        return passengers.stream()
                .map(passenger -> passenger.getFareId() != null ? passenger.getFareChangeFee() : fareChangeFee)
                .map(fee -> fee != null ? fee : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * The refund when this booking is exchanged for a replacement worth {@code replacementValue}: its whole value
     * if its fare is refundable; otherwise only what the replacement uses, and the rest is forfeited.
     */
    public BigDecimal exchangeRefund(BigDecimal replacementValue) {
        BigDecimal value = refundableValue();
        BigDecimal refund = Boolean.TRUE.equals(fareRefundable) ? value : value.min(replacementValue);
        return refund.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_EVEN);
    }
}
