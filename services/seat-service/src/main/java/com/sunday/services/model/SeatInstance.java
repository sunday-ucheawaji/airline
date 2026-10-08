package com.sunday.services.model;

import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "seat_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_id", nullable = false)
    private Long flightId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_instance_cabin_id")
    private FlightInstanceCabin flightInstanceCabin;

    @Column(name = "flight_instance_id")
    private Long flightInstanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatAvailabilityStatus status = SeatAvailabilityStatus.AVAILABLE;

    private String mealPreference;
    private BigDecimal fare;
    private BigDecimal premiumSurcharge;

    // Set while status is HELD; cleared when the hold is confirmed, released, or swept.
    private Instant heldUntil;
    private String heldByBookingReference;

    @Version
    private Long version;

    @Column(name = "flight_schedule_id")
    private Long flightScheduleId;

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
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
