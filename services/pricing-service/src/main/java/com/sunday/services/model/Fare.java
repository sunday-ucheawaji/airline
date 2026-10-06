package com.sunday.services.model;

import com.sunday.common_lib.embeddable.*;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.FareStatus;
import com.sunday.common_lib.enums.PassengerType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fares")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Fare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cross-service ref: Airline is in airline-core-service. Set once from flight-ops at creation.
    @Column(name = "airline_id", nullable = false)
    private Long airlineId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FareStatus status = FareStatus.DRAFT;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Character rbdCode;

    // Cross-service ref: Flight is in flight-ops-service
    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    // Cross-service ref: CabinClass is in seat-service
    @Column(name = "cabin_class_id", nullable = false)
    private Long cabinClassId;

    /**
     * Cabin class type the fare belongs to (ECONOMY, BUSINESS...). Matching key for search and the natural key.
     * Stored here so flight search can filter fares by cabin type without a cross-service join.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "cabin_class", nullable = false, length = 20)
    private CabinClassType cabinClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "passenger_type", nullable = false, length = 10)
    @Builder.Default
    private PassengerType passengerType = PassengerType.ADT;

    @Column(name = "fare_basis_code", length = 20)
    private String fareBasisCode;

    // Pricing: exact decimal, in `currency`
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(precision = 12, scale = 2)
    private BigDecimal taxesAndFees;

    @Column(precision = 12, scale = 2)
    private BigDecimal airlineFees;

    /** base + taxes + airline fees. Stored so search can order and filter in SQL. Set by recalculateTotal(). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 100)
    private String fareLabel;

    // Sales and travel windows
    @Column(name = "sales_from", nullable = false)
    private Instant salesFrom;

    @Column(name = "sales_to", nullable = false)
    private Instant salesTo;

    @Column(name = "travel_from", nullable = false)
    private LocalDate travelFrom;

    @Column(name = "travel_to", nullable = false)
    private LocalDate travelTo;

    @Column(name = "advance_purchase_days")
    private Integer advancePurchaseDays;

    // Same bounded context relationships
    @OneToOne(mappedBy = "fare", cascade = CascadeType.ALL, orphanRemoval = true)
    private BaggagePolicy baggagePolicy;

    @OneToOne(mappedBy = "fare", cascade = CascadeType.ALL, orphanRemoval = true)
    private FareRules fareRules;

    // Embedded benefits
    @Embedded
    @Builder.Default
    private SeatBenefits seatBenefits = new SeatBenefits();

    @Embedded
    @Builder.Default
    private BoardingBenefits boardingBenefits = new BoardingBenefits();

    @Embedded
    @Builder.Default
    private InFlightBenefits inFlightBenefits = new InFlightBenefits();

    @Embedded
    @Builder.Default
    private FlexibilityBenefits flexibilityBenefits = new FlexibilityBenefits();

    @Embedded
    @Builder.Default
    private PremiumServiceBenefits premiumServiceBenefits = new PremiumServiceBenefits();

    // Audit
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void preCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.seatBenefits == null) this.seatBenefits = new SeatBenefits();
        if (this.boardingBenefits == null) this.boardingBenefits = new BoardingBenefits();
        if (this.inFlightBenefits == null) this.inFlightBenefits = new InFlightBenefits();
        if (this.flexibilityBenefits == null) this.flexibilityBenefits = new FlexibilityBenefits();
        if (this.premiumServiceBenefits == null) this.premiumServiceBenefits = new PremiumServiceBenefits();
        recalculateTotal();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
        recalculateTotal();
    }

    /** The only place the stored total is computed. */
    public void recalculateTotal() {
        this.totalAmount = baseFare
                .add(taxesAndFees != null ? taxesAndFees : BigDecimal.ZERO)
                .add(airlineFees != null ? airlineFees : BigDecimal.ZERO);
    }

    /** Business identity of a fare. Mirrors the unique key in V1 and is what duplicate checks compare. */
    public static String keyOf(Long airlineId, Long flightId, CabinClassType cabinClass, Character rbdCode,
                               PassengerType passengerType, String name) {
        return airlineId + ":" + flightId + ":" + cabinClass + ":" + rbdCode + ":" + passengerType + ":" + name;
    }

    public String naturalKey() {
        return keyOf(airlineId, flightId, cabinClass, rbdCode, passengerType, name);
    }
}
