package com.sunday.services.controller;

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
            @PathVariable Long airlineId,
            @Valid @RequestBody AirlineInvitationRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.status(201).body(membershipService.invite(airlineId, request, userId));
    }

    @GetMapping
    public ResponseEntity<List<AirlineMembershipResponse>> listMembers(
            @PathVariable Long airlineId, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(membershipService.listMembers(airlineId, userId));
    }

    /** The invited user accepts their own pending invitation. */
    @PostMapping("/{membershipId}/accept")
    public ResponseEntity<AirlineMembershipResponse> accept(
            @PathVariable Long airlineId, @PathVariable Long membershipId, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(membershipService.accept(airlineId, membershipId, userId));
    }

    @PutMapping("/{membershipId}/role")
    public ResponseEntity<AirlineMembershipResponse> updateRole(
            @PathVariable Long airlineId, @PathVariable Long membershipId,
            @Valid @RequestBody MemberRoleUpdateRequest request, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(membershipService.updateRole(airlineId, membershipId, request, userId));
    }

    @PutMapping("/{membershipId}/status")
    public ResponseEntity<AirlineMembershipResponse> updateStatus(
            @PathVariable Long airlineId, @PathVariable Long membershipId,
            @Valid @RequestBody MemberStatusUpdateRequest request, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(membershipService.updateStatus(airlineId, membershipId, request, userId));
    }

    /** Revokes a pending invite, or removes an active/suspended member — behavior depends on the row's current status. */
    @DeleteMapping("/{membershipId}")
    public ResponseEntity<Void> removeOrRevoke(
            @PathVariable Long airlineId, @PathVariable Long membershipId, @RequestHeader("X-User-Id") Long userId) {
        membershipService.removeOrRevoke(airlineId, membershipId, userId);
        return ResponseEntity.noContent().build();
    }
}
