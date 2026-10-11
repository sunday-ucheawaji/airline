package com.sunday.services.model;

import com.sunday.services.enums.CaseOwnerAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/** Append-only audit trail of who has owned an application's review, and how ownership changed hands. */
@Entity
@Table(name = "onboarding_case_owner_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingCaseOwnerHistory {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AirlineOnboardingApplication application;

    @Column(name = "from_user_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID fromUserId;

    @Column(name = "to_user_id")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID toUserId;

    @Column(name = "actor_user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseOwnerAction action;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant createdAt;
}
