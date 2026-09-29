package com.sunday.services.service.impl;

import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.model.AirlineMembership;
import com.sunday.services.repository.AirlineMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Marks pending invitations EXPIRED once past their {@code expiresAt} — an invite left unanswered forever otherwise. */
@Component
@RequiredArgsConstructor
public class InvitationExpirySweeper {

    private final AirlineMembershipRepository membershipRepository;

    @Scheduled(fixedDelayString = "${airline.invitation-sweep-interval:1h}")
    @Transactional
    public void sweep() {
        List<AirlineMembership> stale = membershipRepository.findByStatusAndExpiresAtBefore(MembershipStatus.INVITED, Instant.now());
        for (AirlineMembership membership : stale) {
            membership.setStatus(MembershipStatus.EXPIRED);
        }
        membershipRepository.saveAll(stale);
    }
}
