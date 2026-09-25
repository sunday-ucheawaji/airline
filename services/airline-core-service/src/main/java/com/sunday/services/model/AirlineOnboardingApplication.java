package com.sunday.services.model;

import com.sunday.services.enums.OnboardingStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * An application submitted by a user to register an airline on the GDS.
 * Deliberately separate from {@link Airline} — no Airline row exists until
 * an application is APPROVED (see design doc section 10.2's "Important Rule":
 * the applicant is not automatically made the owner).
 */
@Entity
@Table(name = "airline_onboarding_applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AirlineOnboardingApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Logical cross-service reference to user-service's User — not a physical FK.
    @Column(name = "applicant_user_id", nullable = false, updatable = false)
    @NotNull
    private Long applicantUserId;

    // Explicitly selected/confirmed during review, not necessarily the applicant.
    @Column(name = "initial_admin_user_id")
    private Long initialAdminUserId;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "iata_code", length = 2)
    private String iataCode;

    @Column(name = "icao_code", length = 3)
    private String icaoCode;

    @Column(nullable = false)
    private String country;

    @Column(name = "registration_number", nullable = false)
    private String registrationNumber;

    private String alliance;
    private String website;
    private String logoUrl;

    @Column(name = "support_email")
    private String supportEmail;
    @Column(name = "support_phone")
    private String supportPhone;
    @Column(name = "support_hours")
    private String supportHours;

    @Column(name = "headquarters_city_id")
    private Long headquartersCityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OnboardingStatus status = OnboardingStatus.DRAFT;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Who gave the final approval — the provisioner must be someone else (separation of duties).
    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    // Set once the application is provisioned into a real Airline.
    @Column(name = "airline_id")
    private Long airlineId;

    private Instant submittedAt;
    private Instant reviewedAt;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
