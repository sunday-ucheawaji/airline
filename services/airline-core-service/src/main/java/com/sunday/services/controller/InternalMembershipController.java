package com.sunday.services.controller;

import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.repository.AirlineMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only: {@code /internal/**} is not routed by the api-gateway. */
@RestController
@RequestMapping("/internal/memberships")
@RequiredArgsConstructor
public class InternalMembershipController {

    private final AirlineMembershipRepository membershipRepository;

    /** True when the user has any membership that has not been removed (invited, active or suspended). */
    @GetMapping("/users/{userId}/exists")
    public boolean hasMembership(@PathVariable Long userId) {
        return membershipRepository.existsByUserIdAndStatusNot(userId, MembershipStatus.REMOVED);
    }
}
