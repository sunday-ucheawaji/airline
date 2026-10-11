package com.sunday.services.repository;

import com.sunday.services.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);

    void deleteByExpiresAtBefore(LocalDateTime cutoff);
}
