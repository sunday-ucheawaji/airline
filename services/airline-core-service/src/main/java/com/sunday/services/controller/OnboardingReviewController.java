package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.OnboardingReviewRequest;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.OnboardingReviewResponse;
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

    @GetMapping
    public ResponseEntity<Page<OnboardingApplicationResponse>> getApplicationsForReview(Pageable pageable) {
        return ResponseEntity.ok(onboardingService.getApplicationsForReview(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OnboardingApplicationResponse> getApplicationForReview(@PathVariable Long id) {
        return ResponseEntity.ok(onboardingService.getApplicationForReview(id));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<OnboardingApplicationResponse> reviewApplication(
            @PathVariable Long id,
            @Valid @RequestBody OnboardingReviewRequest request,
            @RequestHeader("X-User-Id") Long reviewerUserId) {
        return ResponseEntity.ok(onboardingService.reviewApplication(id, request, reviewerUserId));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<OnboardingReviewResponse>> getReviewHistory(@PathVariable Long id) {
        return ResponseEntity.ok(onboardingService.getReviewHistory(id));
    }
}
