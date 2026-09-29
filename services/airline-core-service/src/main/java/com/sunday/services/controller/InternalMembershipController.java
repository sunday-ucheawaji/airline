package com.sunday.services.controller;

import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.repository.AirlineMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumSet;

/** Service-to-service only: {@code /internal/**} is not routed by the api-gateway. */
@RestController
@RequestMapping("/internal/memberships")
@RequiredArgsConstructor
public class InternalMembershipController {

    /** REMOVED/REVOKED/EXPIRED never became (or stopped being) real membership; they don't count as "is a member". */
    private static final EnumSet<MembershipStatus> COUNTS_AS_MEMBER =
            EnumSet.of(MembershipStatus.INVITED, MembershipStatus.ACTIVE, MembershipStatus.SUSPENDED);

    private final AirlineMembershipRepository membershipRepository;

    /** True when the user has any membership that is invited, active or suspended. */
    @GetMapping("/users/{userId}/exists")
    public boolean hasMembership(@PathVariable Long userId) {
        return membershipRepository.existsByUserIdAndStatusIn(userId, COUNTS_AS_MEMBER);
    }
}
