package com.sunday.services.service;

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
    DocumentResponse upload(Long applicationId, Long applicantUserId, DocumentType documentType, MultipartFile file);

    List<DocumentResponse> getMyDocuments(Long applicationId, Long applicantUserId);

    /** A short-lived signed link to one of the applicant's own documents, available once it has passed the checks. */
    DownloadUrlResponse getMyDownloadUrl(Long applicationId, Long documentId, Long applicantUserId);

    /** Removes an upload (record and file). Only while the application is still a draft. */
    void delete(Long applicationId, Long documentId, Long applicantUserId);

    // ----- Staff side: only documents that have passed the automatic checks are visible -----
    List<DocumentResponse> getDocumentsForReview(Long applicationId);

    DownloadUrlResponse getDownloadUrlForReview(Long applicationId, Long documentId);

    DocumentResponse verify(Long applicationId, Long documentId, Long actorUserId);

    DocumentResponse reject(Long applicationId, Long documentId, Long actorUserId, String reason);
}
