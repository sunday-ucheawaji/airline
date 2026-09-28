package com.sunday.services.model;

import com.sunday.services.enums.InformationRequestStatus;
import com.sunday.services.enums.OnboardingStage;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * A question staff put to the applicant while the application stays in review. {@code stage} is the review stage
 * that asked, or null when the case owner asked on behalf of the whole review.
 */
@Entity
@Table(name = "onboarding_information_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingInformationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AirlineOnboardingApplication application;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OnboardingStage stage;

    @Column(name = "requested_by_user_id", nullable = false)
    private Long requestedByUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InformationRequestStatus status = InformationRequestStatus.OPEN;

    @Column(columnDefinition = "TEXT")
    private String response;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
