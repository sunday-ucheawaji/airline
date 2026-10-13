package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.request.OnboardingActionRequest;
import com.sunday.common_lib.payload.response.DocumentResponse;
import com.sunday.common_lib.payload.response.DocumentAccessLogResponse;
import com.sunday.common_lib.payload.response.DownloadUrlResponse;
import com.sunday.services.service.OnboardingDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Staff side of onboarding documents. Only files that passed the automatic checks are listed or downloadable.
 * Each route is gated by its own permission at the api-gateway.
 */
@RestController
@RequestMapping("/api/admin/onboarding/applications/{applicationId}/documents")
@RequiredArgsConstructor
public class OnboardingDocumentReviewController {

    private final OnboardingDocumentService documentService;

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(documentService.getDocumentsForReview(applicationId));
    }

    @GetMapping("/{documentId}/download-url")
    public ResponseEntity<DownloadUrlResponse> getDownloadUrl(
            @PathVariable UUID applicationId, @PathVariable UUID documentId, @RequestHeader("X-User-Id") UUID actorUserId) {
        return ResponseEntity.ok(documentService.getDownloadUrlForReview(applicationId, documentId, actorUserId));
    }

    @GetMapping("/{documentId}/access-log")
    public ResponseEntity<List<DocumentAccessLogResponse>> getAccessLog(@PathVariable UUID applicationId, @PathVariable UUID documentId) {
        return ResponseEntity.ok(documentService.getAccessLog(applicationId, documentId));
    }

    @PostMapping("/{documentId}/verify")
    public ResponseEntity<DocumentResponse> verify(
            @PathVariable UUID applicationId, @PathVariable UUID documentId, @RequestHeader("X-User-Id") UUID actorUserId) {
        return ResponseEntity.ok(documentService.verify(applicationId, documentId, actorUserId));
    }

    @PostMapping("/{documentId}/reject")
    public ResponseEntity<DocumentResponse> reject(
            @PathVariable UUID applicationId,
            @PathVariable UUID documentId,
            @RequestBody(required = false) OnboardingActionRequest request,
            @RequestHeader("X-User-Id") UUID actorUserId) {
        return ResponseEntity.ok(documentService.reject(applicationId, documentId, actorUserId, request == null ? null : request.getComments()));
    }
}
