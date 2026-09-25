package com.sunday.services.repository;

import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.model.AirlineMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AirlineMembershipRepository extends JpaRepository<AirlineMembership, Long> {

    List<AirlineMembership> findByAirlineId(Long airlineId);

    Optional<AirlineMembership> findByUserIdAndAirlineId(Long userId, Long airlineId);

    List<AirlineMembership> findByUserIdAndStatus(Long userId, MembershipStatus status);

    boolean existsByUserIdAndStatusNot(Long userId, MembershipStatus status);

    boolean existsByUserIdAndAirlineIdAndStatus(Long userId, Long airlineId, MembershipStatus status);
}
