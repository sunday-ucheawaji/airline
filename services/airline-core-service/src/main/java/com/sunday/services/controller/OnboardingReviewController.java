package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.OnboardingActionRequest;
import com.sunday.common_lib.payload.request.OwnerAssignmentRequest;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.OnboardingReviewResponse;
import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/onboarding/applications")
@RequiredArgsConstructor
public class OnboardingReviewController {

    private final OnboardingService onboardingService;

    /** Defaults to the SUBMITTED queue; pass {@code status=APPROVED} for applications awaiting provisioning. */
    @GetMapping
    public ResponseEntity<Page<OnboardingApplicationResponse>> getApplicationsForReview(
            @RequestParam(required = false) OnboardingStatus status, Pageable pageable) {
        return ResponseEntity.ok(onboardingService.getApplicationsForReview(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OnboardingApplicationResponse> getApplicationForReview(@PathVariable Long id) {
        return ResponseEntity.ok(onboardingService.getApplicationForReview(id));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<OnboardingReviewResponse>> getReviewHistory(@PathVariable Long id) {
        return ResponseEntity.ok(onboardingService.getReviewHistory(id));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<OnboardingApplicationResponse> returnApplication(
            @PathVariable Long id,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(onboardingService.returnApplication(id, comments(request), actorUserId));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<OnboardingApplicationResponse> approveApplication(
            @PathVariable Long id,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(onboardingService.approveApplication(id, comments(request), actorUserId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OnboardingApplicationResponse> rejectApplication(
            @PathVariable Long id,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(onboardingService.rejectApplication(id, comments(request), actorUserId));
    }

    @PutMapping("/{id}/owner")
    public ResponseEntity<OnboardingApplicationResponse> assignOwner(
            @PathVariable Long id,
            @Valid @RequestBody OwnerAssignmentRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(onboardingService.assignOwner(id, request, actorUserId));
    }

    @PostMapping("/{id}/provision")
    public ResponseEntity<OnboardingApplicationResponse> provisionApplication(
            @PathVariable Long id,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(onboardingService.provisionApplication(id, comments(request), actorUserId));
    }

    private static String comments(OnboardingActionRequest request) {
        return request == null ? null : request.getComments();
    }
}
