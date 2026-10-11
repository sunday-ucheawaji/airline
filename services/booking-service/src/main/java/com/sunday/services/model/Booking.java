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

    // Cross-service ref: User (user-service)
    @Column(name = "user_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    // Cross-service ref: Flight (flight-ops-service)
    @Column(name = "flight_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID flightId;

    // Cross-service ref: FlightInstance (flight-ops-service)
    @Column(name = "flight_instance_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID flightInstanceId;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID airlineId;

    private TripType tripType = TripType.ONE_WAY;

    @Enumerated(EnumType.STRING)
    private CabinClassType cabinClass = CabinClassType.ECONOMY;

    // Cross-service ref: Fare (pricing-service)
    @Column(name = "fare_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID fareId;

    // Fare as priced at purchase time, so a later repricing or rule edit does not change this booking.
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

    // What the customer owes for this booking, fixed at creation. Payment must match it.
    private BigDecimal totalAmount;

    private boolean flexibleTicket;
    private LocalDateTime ticketTimeLimit;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Passenger> passengers = new HashSet<>();

    // Cross-service ref: SeatInstance IDs (seat-service)
    @ElementCollection
    @CollectionTable(name = "booking_seat_instances", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_instance_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private List<UUID> seatInstanceIds;

    // Cross-service ref: FlightCabinAncillary IDs (ancillary-service)
    @ElementCollection
    @CollectionTable(name = "booking_ancillaries", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "ancillary_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private List<UUID> ancillaryIds;

    // Cross-service ref: FlightMeal IDs (ancillary-service)
    @ElementCollection
    @CollectionTable(name = "booking_meals",
            joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "meal_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private List<UUID> mealIds;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    private Set<Ticket> tickets=new HashSet<>();

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
}
