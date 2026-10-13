package com.sunday.services.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** 1 if this call revoked the token, 0 if it was already revoked (a concurrent refresh got there first). */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now, t.lastUsedAt = :now WHERE t.id = :id AND t.revokedAt IS NULL")
    int revokeIfActive(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
