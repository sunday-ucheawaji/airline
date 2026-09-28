package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.InformationResponseRequest;
import com.sunday.common_lib.payload.request.OnboardingApplicationRequest;
import com.sunday.common_lib.payload.response.InformationRequestResponse;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.validation.OnCreate;
import com.sunday.services.service.OnboardingService;
import com.sunday.services.service.OnboardingWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/onboarding/applications")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final OnboardingWorkflowService workflowService;

    @PostMapping
    public ResponseEntity<OnboardingApplicationResponse> createDraft(
            @Validated(OnCreate.class) @RequestBody OnboardingApplicationRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        OnboardingApplicationResponse response = onboardingService.createDraft(request, userId);
        return ResponseEntity.created(URI.create("/api/onboarding/applications/" + response.getId())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<OnboardingApplicationResponse>> getMyApplications(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(onboardingService.getMyApplications(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OnboardingApplicationResponse> getMyApplicationById(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(onboardingService.getMyApplicationById(id, userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OnboardingApplicationResponse> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody OnboardingApplicationRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(onboardingService.updateDraft(id, request, userId));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<OnboardingApplicationResponse> withdraw(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(workflowService.withdraw(id, userId));
    }

    @GetMapping("/{id}/information-requests")
    public ResponseEntity<List<InformationRequestResponse>> getMyInformationRequests(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(workflowService.getMyInformationRequests(id, userId));
    }

    @PostMapping("/{id}/information-requests/{requestId}/respond")
    public ResponseEntity<InformationRequestResponse> respond(
            @PathVariable Long id,
            @PathVariable Long requestId,
            @Valid @RequestBody InformationResponseRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(workflowService.respond(id, requestId, userId, request));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<OnboardingApplicationResponse> submitApplication(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(onboardingService.submitApplication(id, userId));
    }
}
