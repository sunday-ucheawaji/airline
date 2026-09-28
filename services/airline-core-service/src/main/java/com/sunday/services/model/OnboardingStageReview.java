package com.sunday.services.model;

import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.enums.StageStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * One specialist review stage of an application. Three rows exist per application (one per
 * {@link OnboardingStage}), created on submission and reset on every resubmission.
 */
@Entity
@Table(
        name = "onboarding_stage_reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "stage"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStageReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AirlineOnboardingApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OnboardingStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StageStatus status = StageStatus.PENDING;

    // Logical cross-service references to user-service's User — not physical FKs.
    @Column(name = "assignee_user_id")
    private Long assigneeUserId;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
