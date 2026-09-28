package com.sunday.services.controller;

import com.sunday.common_lib.payload.response.ReviewerResponse;
import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.service.OnboardingWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lets a case owner see who can be picked: officers (for a transfer) or the specialists of one stage. */
@RestController
@RequestMapping("/api/admin/onboarding/reviewers")
@RequiredArgsConstructor
public class OnboardingReviewerController {

    private final OnboardingWorkflowService workflowService;

    /** No {@code stage}: onboarding officers. {@code stage=compliance|commercial|technical}: that stage's specialists. */
    @GetMapping
    public ResponseEntity<List<ReviewerResponse>> getReviewers(@RequestParam(required = false) String stage) {
        OnboardingStage parsed = stage == null ? null : OnboardingStage.fromPath(stage);
        return ResponseEntity.ok(workflowService.getReviewers(parsed));
    }
}
