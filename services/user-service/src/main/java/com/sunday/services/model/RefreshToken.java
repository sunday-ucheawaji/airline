package com.sunday.services.model;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = ErrorMessageUtil.USER_MANDATORY)
    private User user;

    @Column(nullable = false, unique = true)
    @NotBlank(message = ErrorMessageUtil.TOKEN_HASH_MANDATORY)
    private String tokenHash;

    @Column(nullable = false)
    @NotNull(message = ErrorMessageUtil.EXPIRES_AT_MANDATORY)
    private LocalDateTime expiresAt;

    private LocalDateTime revokedAt;

    private LocalDateTime lastUsedAt;

    private String userAgent;

    private String ipAddress;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
