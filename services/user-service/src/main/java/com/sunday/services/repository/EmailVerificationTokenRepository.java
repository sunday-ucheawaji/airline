package com.sunday.services.repository;

import com.sunday.services.model.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findByUserIdAndUsedAtIsNull(UUID userId);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
