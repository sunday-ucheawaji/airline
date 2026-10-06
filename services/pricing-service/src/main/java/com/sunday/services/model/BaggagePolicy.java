package com.sunday.services.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "baggage_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class BaggagePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fare_id", nullable = false)
    @JsonIgnore
    private Fare fare;

    @Column(nullable = false)
    private String name;

    private String description;

    // Cabin baggage
    private Double cabinBaggageMaxWeight;

    @Builder.Default
    private Integer cabinBaggagePieces = 1;

    private Double cabinBaggageWeightPerPiece;
    private Double cabinBaggageMaxDimension;

    // Check-in baggage
    private Double checkInBaggageMaxWeight;

    @Builder.Default
    private Integer checkInBaggagePieces = 1;

    private Double checkInBaggageWeightPerPiece;

    @Builder.Default
    private Integer freeCheckedBagsAllowance = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean priorityBaggage = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean extraBaggageAllowance = false;

    // Cross-service ref: Airline. Copied from the fare on write, never taken from the request.
    @Column(name = "airline_id", nullable = false)
    private Long airlineId;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    // Audit
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void preCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
