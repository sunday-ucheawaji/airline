package com.sunday.services.controller;

import com.sunday.common_lib.payload.response.DocumentResponse;
import com.sunday.common_lib.payload.response.DownloadUrlResponse;
import com.sunday.services.enums.DocumentType;
import com.sunday.services.service.OnboardingDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** The applicant's side of onboarding documents. */
@RestController
@RequestMapping("/api/onboarding/applications/{applicationId}/documents")
@RequiredArgsConstructor
public class OnboardingDocumentController {

    private final OnboardingDocumentService documentService;

    /** Answers 202: the file is saved to quarantine and inspected asynchronously; poll the list for the outcome. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @PathVariable Long applicationId,
            @RequestParam("documentType") DocumentType documentType,
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.accepted().body(documentService.upload(applicationId, userId, documentType, file));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getMyDocuments(
            @PathVariable Long applicationId, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(documentService.getMyDocuments(applicationId, userId));
    }

    /** A short-lived signed link; the file is never public. Available once the document has passed the checks. */
    @GetMapping("/{documentId}/download-url")
    public ResponseEntity<DownloadUrlResponse> getDownloadUrl(
            @PathVariable Long applicationId, @PathVariable Long documentId, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(documentService.getMyDownloadUrl(applicationId, documentId, userId));
    }

    /** Only while the application is still a draft. */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long applicationId, @PathVariable Long documentId, @RequestHeader("X-User-Id") Long userId) {
        documentService.delete(applicationId, documentId, userId);
        return ResponseEntity.noContent().build();
    }
}
