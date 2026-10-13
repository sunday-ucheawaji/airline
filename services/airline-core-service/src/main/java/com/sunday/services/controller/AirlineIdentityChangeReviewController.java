package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.OnboardingActionRequest;
import com.sunday.common_lib.payload.response.AirlineIdentityChangeResponse;
import com.sunday.services.enums.IdentityChangeStatus;
import com.sunday.services.service.AirlineIdentityChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Staff side of identity change requests. Each route is gated by its own permission at the api-gateway. */
@RestController
@RequestMapping("/api/admin/airlines/identity-change-requests")
@RequiredArgsConstructor
public class AirlineIdentityChangeReviewController {

    private final AirlineIdentityChangeService identityChangeService;

    @GetMapping
    public ResponseEntity<Page<AirlineIdentityChangeResponse>> list(
            @RequestParam(required = false) IdentityChangeStatus status, Pageable pageable) {
        return ResponseEntity.ok(identityChangeService.listForReview(status, pageable));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<AirlineIdentityChangeResponse> get(@PathVariable UUID requestId) {
        return ResponseEntity.ok(identityChangeService.getForReview(requestId));
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<AirlineIdentityChangeResponse> approve(
            @PathVariable UUID requestId,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") UUID reviewerUserId) {
        return ResponseEntity.ok(identityChangeService.approve(requestId, reviewerUserId, comments(request)));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<AirlineIdentityChangeResponse> reject(
            @PathVariable UUID requestId,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") UUID reviewerUserId) {
        return ResponseEntity.ok(identityChangeService.reject(requestId, reviewerUserId, comments(request)));
    }

    private static String comments(OnboardingActionRequest request) {
        return request == null ? null : request.getComments();
    }
}
