package com.sunday.services.model;

import com.sunday.common_lib.enums.ReviewDecision;
import com.sunday.services.enums.OnboardingStage;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Append-only review history for an onboarding application — never updated
 * or deleted, only inserted, so the system retains a full audit trail even
 * across multiple review rounds (e.g. REQUESTED_CHANGES then a later APPROVED).
 */
@Entity
@Table(name = "onboarding_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    @NotNull
    private AirlineOnboardingApplication application;

    // Who performed the action this entry records (staff, or the applicant for INFORMATION_PROVIDED / WITHDRAWN).
    // Logical cross-service reference to user-service's User — not a physical FK.
    @Column(name = "actor_user_id", nullable = false)
    @NotNull
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comments;

    // The user the entry is about: the nominated airline owner (OWNER_ASSIGNED / PROVISIONED) or a stage assignee.
    @Column(name = "target_user_id")
    private Long targetUserId;

    // The review stage the entry belongs to; null when it is about the whole review (e.g. the case owner acting).
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OnboardingStage stage;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
