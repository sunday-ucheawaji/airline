package com.sunday.services.model;

import com.sunday.common_lib.enums.Gender;
import com.sunday.common_lib.enums.PassengerType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

@Entity
@Table(name = "passengers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Passenger {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @NotBlank
    @Column(nullable = false)
    private String firstName;

    @NotBlank
    @Column(nullable = false)
    private String lastName;

    @Email
    private String email;

    private String phone;

    @NotNull
    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column
    private String passportNumber;

    private String nationality;
    private String frequentFlyerNumber;

    @Column(name = "primary_user_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID primaryUserId;

    @Column(name = "seat_instance_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID seatInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "passenger_type", nullable = false)
    private PassengerType passengerType;

    // Cross-service ref: the pricing-service fare that priced this passenger, which for a child or infant can be
    // the fare filed alongside the booking's adult fare.
    @Column(name = "fare_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID fareId;

    @Column(name = "fare_amount", precision = 12, scale = 2)
    private BigDecimal fareAmount;

    @Column(name = "fare_cancellation_fee", precision = 12, scale = 2)
    private BigDecimal fareCancellationFee;

    @Column(name = "fare_change_fee", precision = 12, scale = 2)
    private BigDecimal fareChangeFee;

    @Builder.Default
    private Boolean requiresWheelchairAssistance = false;
    private String dietaryPreferences;
    private String medicalConditions;

    @ManyToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /** IATA age bands on the day of travel: an infant is under 2, a child under 12, everyone else an adult. */
    public static PassengerType typeOn(LocalDate dateOfBirth, LocalDate travelDate) {
        int age = Period.between(dateOfBirth, travelDate).getYears();
        if (age < 2) return PassengerType.INF;
        if (age < 12) return PassengerType.CHD;
        return PassengerType.ADT;
    }

    /** An infant without a seat travels on an adult's lap. */
    public boolean isLapInfant() {
        return passengerType == PassengerType.INF && seatInstanceId == null;
    }

    /** The fare type a passenger is priced at: an infant who takes a seat pays as a child. */
    public static PassengerType pricingType(PassengerType type, UUID seatInstanceId) {
        return type == PassengerType.INF && seatInstanceId != null ? PassengerType.CHD : type;
    }

    public boolean isAdult() {
        return getAge() >= 18;
    }

    @PrePersist
    @PreUpdate
    private void normalizeData() {
        this.email = email != null ? email.toLowerCase().trim() : null;
        this.firstName = capitalizeFirstLetter(firstName);
        this.lastName = capitalizeFirstLetter(lastName);
    }

    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
