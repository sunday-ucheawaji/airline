package com.sunday.services.repository;

import com.sunday.services.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(UUID userId);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
