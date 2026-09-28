package com.sunday.services.controller;

import com.sunday.common_lib.payload.request.CaseOwnerChangeRequest;
import com.sunday.common_lib.payload.request.InformationRequestCreateRequest;
import com.sunday.common_lib.payload.request.OnboardingActionRequest;
import com.sunday.common_lib.payload.request.OnboardingCommentRequest;
import com.sunday.common_lib.payload.request.StageAssigneeRequest;
import com.sunday.common_lib.payload.response.CaseOwnerHistoryResponse;
import com.sunday.common_lib.payload.response.InformationRequestResponse;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.OnboardingReviewResponse;
import com.sunday.common_lib.payload.response.OnboardingStageResponse;
import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.service.OnboardingWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Staged review: case ownership, specialist stages, information requests, comments, and the hand-off to final
 * approval. Every route is gated by its own permission at the api-gateway; who-may-do-what on a given case
 * (owner, assigned specialist) is enforced in the service.
 */
@RestController
@RequestMapping("/api/admin/onboarding/applications")
@RequiredArgsConstructor
public class OnboardingWorkflowController {

    private final OnboardingWorkflowService workflowService;

    // ---------- Case ownership ----------

    @PostMapping("/{id}/claim")
    public ResponseEntity<OnboardingApplicationResponse> claim(
            @PathVariable Long id, @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.claim(id, actorUserId));
    }

    @PutMapping("/{id}/case-owner")
    public ResponseEntity<OnboardingApplicationResponse> transfer(
            @PathVariable Long id,
            @Valid @RequestBody CaseOwnerChangeRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.transfer(id, actorUserId, request));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<OnboardingApplicationResponse> release(
            @PathVariable Long id,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.release(id, actorUserId, comments(request)));
    }

    /** Super admin only (checked against the forwarded roles): forces the case to another officer, reason required. */
    @PostMapping("/{id}/take-over")
    public ResponseEntity<OnboardingApplicationResponse> takeOver(
            @PathVariable Long id,
            @Valid @RequestBody CaseOwnerChangeRequest request,
            @RequestHeader("X-User-Id") Long actorUserId,
            @RequestHeader(value = "X-User-Roles", defaultValue = "") String actorRoles) {
        return ResponseEntity.ok(workflowService.takeOver(id, actorUserId, parseRoles(actorRoles), request));
    }

    @GetMapping("/{id}/case-owner-history")
    public ResponseEntity<List<CaseOwnerHistoryResponse>> getCaseOwnerHistory(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getCaseOwnerHistory(id));
    }

    // ---------- Stages ----------

    @GetMapping("/{id}/stages")
    public ResponseEntity<List<OnboardingStageResponse>> getStages(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getStages(id));
    }

    @PutMapping("/{id}/stages/{stage}/assignee")
    public ResponseEntity<OnboardingStageResponse> assignStage(
            @PathVariable Long id,
            @PathVariable String stage,
            @Valid @RequestBody StageAssigneeRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.assignStage(id, OnboardingStage.fromPath(stage), actorUserId, request));
    }

    @PostMapping("/{id}/stages/{stage}/approve")
    public ResponseEntity<OnboardingStageResponse> approveStage(
            @PathVariable Long id,
            @PathVariable String stage,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.decideStage(id, OnboardingStage.fromPath(stage), true, actorUserId, comments(request)));
    }

    @PostMapping("/{id}/stages/{stage}/reject")
    public ResponseEntity<OnboardingStageResponse> rejectStage(
            @PathVariable Long id,
            @PathVariable String stage,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.decideStage(id, OnboardingStage.fromPath(stage), false, actorUserId, comments(request)));
    }

    // ---------- Information requests and comments ----------

    @GetMapping("/{id}/information-requests")
    public ResponseEntity<List<InformationRequestResponse>> getInformationRequests(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getInformationRequests(id));
    }

    /** The case owner asking the applicant on behalf of the whole review. */
    @PostMapping("/{id}/information-requests")
    public ResponseEntity<InformationRequestResponse> requestInformation(
            @PathVariable Long id,
            @Valid @RequestBody InformationRequestCreateRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.requestInformation(id, null, actorUserId, request.getMessage()));
    }

    /** An assigned specialist asking the applicant about their own stage. */
    @PostMapping("/{id}/stages/{stage}/information-requests")
    public ResponseEntity<InformationRequestResponse> requestStageInformation(
            @PathVariable Long id,
            @PathVariable String stage,
            @Valid @RequestBody InformationRequestCreateRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.requestInformation(id, OnboardingStage.fromPath(stage), actorUserId, request.getMessage()));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<OnboardingReviewResponse> comment(
            @PathVariable Long id,
            @Valid @RequestBody OnboardingCommentRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.comment(id, actorUserId, request.getMessage()));
    }

    // ---------- Hand-off to final approval ----------

    @PostMapping("/{id}/refer")
    public ResponseEntity<OnboardingApplicationResponse> refer(
            @PathVariable Long id,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.refer(id, actorUserId, comments(request)));
    }

    @PostMapping("/{id}/send-back")
    public ResponseEntity<OnboardingApplicationResponse> sendBack(
            @PathVariable Long id,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") Long actorUserId) {
        return ResponseEntity.ok(workflowService.sendBack(id, actorUserId, comments(request)));
    }

    private static String comments(OnboardingActionRequest request) {
        return request == null ? null : request.getComments();
    }

    private static List<String> parseRoles(String header) {
        return Arrays.stream(header.split(",")).map(String::trim).filter(role -> !role.isEmpty()).toList();
    }
}
