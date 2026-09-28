package com.sunday.services.mapper;

import com.sunday.common_lib.event.OnboardingDocumentUploadedEvent;
import com.sunday.common_lib.payload.response.DocumentResponse;
import com.sunday.services.model.OnboardingDocument;

import java.util.List;

public class DocumentMapper {

    public static DocumentResponse toResponse(OnboardingDocument document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .applicationId(document.getApplication().getId())
                .documentType(document.getDocumentType().name())
                .originalFileName(document.getOriginalFileName())
                .status(document.getStatus().name())
                .detectedContentType(document.getDetectedContentType())
                .fileSize(document.getFileSize())
                .checksumSha256(document.getChecksumSha256())
                .blockedReason(document.getBlockedReason())
                .uploadedByUserId(document.getUploadedByUserId())
                .verifiedByUserId(document.getVerifiedByUserId())
                .verifiedAt(document.getVerifiedAt())
                .rejectionReason(document.getRejectionReason())
                .createdAt(document.getCreatedAt())
                .build();
    }

    public static List<DocumentResponse> toResponseList(List<OnboardingDocument> documents) {
        return documents.stream().map(DocumentMapper::toResponse).toList();
    }

    /** The Kafka message that asks the processor to inspect an upload that is sitting in quarantine. */
    public static OnboardingDocumentUploadedEvent toUploadedEvent(OnboardingDocument document) {
        return OnboardingDocumentUploadedEvent.builder()
                .documentId(document.getId())
                .applicationId(document.getApplication().getId())
                .quarantineKey(document.getStorageKey())
                .declaredContentType(document.getDeclaredContentType())
                .declaredFileName(document.getOriginalFileName())
                .uploadedByUserId(document.getUploadedByUserId())
                .uploadedAt(document.getCreatedAt())
                .build();
    }
}
