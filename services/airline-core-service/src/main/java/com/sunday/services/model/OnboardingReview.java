package com.sunday.services.model;

import com.sunday.common_lib.enums.ReviewDecision;
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

    // Logical cross-service reference to user-service's User — not a physical FK.
    @Column(name = "reviewer_user_id", nullable = false)
    @NotNull
    private Long reviewerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
