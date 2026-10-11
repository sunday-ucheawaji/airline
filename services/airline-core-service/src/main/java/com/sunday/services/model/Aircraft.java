package com.sunday.services.model;

import com.sunday.common_lib.enums.AircraftStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "aircrafts",
        indexes = {
                @Index(name = "idx_aircraft_code", columnList = "aircraft_code"),
                @Index(name = "idx_aircraft_model", columnList = "model"),
                @Index(name = "idx_aircraft_airline", columnList = "airline_id")
        })
public class Aircraft {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(updatable = false, nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @NotBlank(message = "Aircraft code is required")
    @Column(nullable = false, unique = true, name = "aircraft_code", length = 20)
    private String code;

    @NotBlank(message = "Aircraft model is required")
    @Column(nullable = false, name = "model", length = 50)
    private String model;

    @NotBlank(message = "Manufacturer is required")
    @Column(nullable = false, length = 50)
    private String manufacturer;

    @NotNull(message = "Seating capacity is required")
    @Positive(message = "Seating capacity must be positive")
    @Column(nullable = false)
    private Integer seatingCapacity;

    @Column(name = "economy_seats")
    private Integer economySeats;

    @Column(name = "premium_economy_seats")
    private Integer premiumEconomySeats;

    @Column(name = "business_seats")
    private Integer businessSeats;

    @Column(name = "first_class_seats")
    private Integer firstClassSeats;

    @Positive(message = "Range must be positive")
    @Column(name = "range_km")
    private Integer rangeKm;

    @Column(name = "cruising_speed_kmh")
    private Integer cruisingSpeedKmh;

    @Column(name = "max_altitude_ft")
    private Integer maxAltitudeFt;

    @Column(name = "year_of_manufacture")
    private Integer yearOfManufacture;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    @Column(name = "next_maintenance_date")
    private LocalDate nextMaintenanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AircraftStatus status = AircraftStatus.ACTIVE;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    // Same bounded context — direct JPA relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id", nullable = false)
    @NotNull(message = "Airline is required")
    private Airline airline;

    // Cross-service reference: stored as ID (Airport lives in another service)
    @Column(name = "current_airport_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID currentAirportId;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Integer getTotalSeats() {
        return (economySeats != null ? economySeats : 0) +
                (premiumEconomySeats != null ? premiumEconomySeats : 0) +
                (businessSeats != null ? businessSeats : 0) +
                (firstClassSeats != null ? firstClassSeats : 0);
    }

    public boolean isOperational() {
        return AircraftStatus.ACTIVE.equals(status)
                && Boolean.TRUE.equals(isAvailable);
    }

    public boolean requiresMaintenance() {
        return nextMaintenanceDate != null
                && nextMaintenanceDate.isBefore(
                        LocalDate.now().plusWeeks(2));
    }
}
