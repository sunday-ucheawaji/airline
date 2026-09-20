package com.sunday.services.model;

import com.sunday.services.enums.MembershipStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * A user's membership in a specific airline organization — the central entity
 * supporting multi-airline access. {@code userId}/{@code roleId} are logical
 * cross-service references to user-service's User/Role, not physical foreign
 * keys (user-service owns that data, in its own database).
 */
@Entity
@Table(
        name = "airline_memberships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "airline_id"})
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class AirlineMembership {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "airline_id", nullable = false)
    @NotNull
    private Airline airline;

    @Column(name = "user_id", nullable = false)
    @NotNull
    private Long userId;

    @Column(name = "role_id", nullable = false)
    @NotNull
    private Long roleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    private Instant joinedAt;

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
