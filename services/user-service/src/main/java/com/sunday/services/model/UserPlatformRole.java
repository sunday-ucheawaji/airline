package com.sunday.services.model;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import java.util.UUID;

/**
 * Direct grant of a PLATFORM-scoped {@link Role} to a {@link User}. AIRLINE-scoped
 * roles are (eventually) granted through AirlineMembership instead — this table
 * exists only because there's no membership concept for platform-wide authority
 * (e.g. reviewing onboarding applications) to hang off.
 */
@Entity
@Table(
        name = "user_platform_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"})
)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserPlatformRole {

    @Id
    @EqualsAndHashCode.Include
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = ErrorMessageUtil.USER_MANDATORY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    @NotNull(message = ErrorMessageUtil.ROLE_MANDATORY)
    private Role role;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
