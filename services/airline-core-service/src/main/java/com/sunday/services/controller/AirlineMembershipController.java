package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.request.AirlineInvitationRequest;
import com.sunday.common_lib.payload.request.MemberRoleUpdateRequest;
import com.sunday.common_lib.payload.request.MemberStatusUpdateRequest;
import com.sunday.common_lib.payload.response.AirlineMembershipResponse;
import com.sunday.services.service.AirlineMembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** An airline's team: invitations, roster, and role/status changes. Permission-gated inside the service. */
@RestController
@RequestMapping("/api/airlines/{airlineId}/members")
@RequiredArgsConstructor
public class AirlineMembershipController {

    private final AirlineMembershipService membershipService;

    @PostMapping("/invitations")
    public ResponseEntity<AirlineMembershipResponse> invite(
            @PathVariable UUID airlineId,
            @Valid @RequestBody AirlineInvitationRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.status(201).body(membershipService.invite(airlineId, request, userId));
    }

    @GetMapping
    public ResponseEntity<List<AirlineMembershipResponse>> listMembers(
            @PathVariable UUID airlineId, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(membershipService.listMembers(airlineId, userId));
    }

    /** The invited user accepts their own pending invitation. */
    @PostMapping("/{membershipId}/accept")
    public ResponseEntity<AirlineMembershipResponse> accept(
            @PathVariable UUID airlineId, @PathVariable UUID membershipId, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(membershipService.accept(airlineId, membershipId, userId));
    }

    @PutMapping("/{membershipId}/role")
    public ResponseEntity<AirlineMembershipResponse> updateRole(
            @PathVariable UUID airlineId, @PathVariable UUID membershipId,
            @Valid @RequestBody MemberRoleUpdateRequest request, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(membershipService.updateRole(airlineId, membershipId, request, userId));
    }

    @PutMapping("/{membershipId}/status")
    public ResponseEntity<AirlineMembershipResponse> updateStatus(
            @PathVariable UUID airlineId, @PathVariable UUID membershipId,
            @Valid @RequestBody MemberStatusUpdateRequest request, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(membershipService.updateStatus(airlineId, membershipId, request, userId));
    }

    /** Revokes a pending invite, or removes an active/suspended member — behavior depends on the row's current status. */
    @DeleteMapping("/{membershipId}")
    public ResponseEntity<Void> removeOrRevoke(
            @PathVariable UUID airlineId, @PathVariable UUID membershipId, @RequestHeader("X-User-Id") UUID userId) {
        membershipService.removeOrRevoke(airlineId, membershipId, userId);
        return ResponseEntity.noContent().build();
    }
}
