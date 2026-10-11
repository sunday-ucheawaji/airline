package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.model.AirlineMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AirlineMembershipRepository extends JpaRepository<AirlineMembership, UUID> {

    List<AirlineMembership> findByAirlineId(UUID airlineId);

    Optional<AirlineMembership> findByUserIdAndAirlineId(UUID userId, UUID airlineId);

    List<AirlineMembership> findByUserIdAndStatus(UUID userId, MembershipStatus status);

    boolean existsByUserIdAndStatusNot(UUID userId, MembershipStatus status);

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<MembershipStatus> statuses);

    boolean existsByUserIdAndAirlineIdAndStatus(UUID userId, UUID airlineId, MembershipStatus status);

    long countByAirlineIdAndRoleIdAndStatus(UUID airlineId, UUID roleId, MembershipStatus status);

    List<AirlineMembership> findByStatusAndExpiresAtBefore(MembershipStatus status, Instant cutoff);
}
