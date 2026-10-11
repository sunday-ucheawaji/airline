package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.response.DocumentResponse;
import com.sunday.common_lib.payload.response.DownloadUrlResponse;
import com.sunday.services.enums.DocumentType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Onboarding documents: upload and listing for the applicant. Staff-side actions are added alongside. */
public interface OnboardingDocumentService {

    /**
     * Accepts an upload: cheap checks, raw file into quarantine, a {@code PROCESSING} record, and an event for the
     * asynchronous inspection. Returns straight away; the applicant sees the outcome when listing.
     */
    DocumentResponse upload(UUID applicationId, UUID applicantUserId, DocumentType documentType, MultipartFile file);

    List<DocumentResponse> getMyDocuments(UUID applicationId, UUID applicantUserId);

    /** A short-lived signed link to one of the applicant's own documents, available once it has passed the checks. */
    DownloadUrlResponse getMyDownloadUrl(UUID applicationId, UUID documentId, UUID applicantUserId);

    /** Removes an upload (record and file). Only while the application is still a draft. */
    void delete(UUID applicationId, UUID documentId, UUID applicantUserId);

    // ----- Staff side: only documents that have passed the automatic checks are visible -----
    List<DocumentResponse> getDocumentsForReview(UUID applicationId);

    DownloadUrlResponse getDownloadUrlForReview(UUID applicationId, UUID documentId);

    DocumentResponse verify(UUID applicationId, UUID documentId, UUID actorUserId);

    DocumentResponse reject(UUID applicationId, UUID documentId, UUID actorUserId, String reason);
}
