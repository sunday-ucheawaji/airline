package com.sunday.services.controller;

import com.sunday.services.service.AirlineMembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Service-to-service only: {@code /internal/**} is not routed by the api-gateway. */
@RestController
@RequestMapping("/internal/memberships")
@RequiredArgsConstructor
public class InternalMembershipController {

    private final AirlineMembershipService membershipService;

    @GetMapping("/users/{userId}/exists")
    public boolean hasMembership(@PathVariable UUID userId) {
        return membershipService.hasMembership(userId);
    }
}
