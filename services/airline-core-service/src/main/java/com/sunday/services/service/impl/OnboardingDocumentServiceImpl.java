package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.ReviewDecision;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.response.DocumentResponse;
import com.sunday.common_lib.payload.response.DownloadUrlResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.config.DocumentProperties;
import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.enums.DocumentType;
import com.sunday.services.enums.InformationRequestStatus;
import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.mapper.DocumentMapper;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingDocument;
import com.sunday.services.repository.OnboardingDocumentRepository;
import com.sunday.services.repository.OnboardingInformationRequestRepository;
import com.sunday.services.service.OnboardingDocumentService;
import com.sunday.services.storage.DocumentStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingDocumentServiceImpl implements OnboardingDocumentService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingDocumentServiceImpl.class);
    private static final Set<DocumentStatus> VIEWABLE = Set.of(DocumentStatus.CLEAN, DocumentStatus.VERIFIED, DocumentStatus.REJECTED);
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 150;

    private final OnboardingSupport support;
    private final OnboardingDocumentRepository documentRepository;
    private final OnboardingInformationRequestRepository informationRequestRepository;
    private final DocumentStorage storage;
    private final DocumentProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public DocumentResponse upload(Long applicationId, Long applicantUserId, DocumentType documentType, MultipartFile file) {
        AirlineOnboardingApplication application = support.getOrThrow(applicationId);
        support.requireOwnedByApplicant(application, applicantUserId);
        requireUploadsAccepted(application);
        requireAcceptableFile(file);
        if (documentRepository.countByApplicationId(applicationId) >= properties.getMaxDocumentsPerApplication()) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.DOCUMENT_LIMIT_REACHED, applicationId, properties.getMaxDocumentsPerApplication()));
        }

        // The key is generated, never derived from anything the uploader sent.
        String key = "onboarding/" + applicationId + "/" + UUID.randomUUID();
        try (InputStream in = file.getInputStream()) {
            storage.putQuarantine(key, in, file.getSize());
        } catch (IOException e) {
            throw new BadRequestException(ErrorMessageUtil.DOCUMENT_FILE_MANDATORY);
        }

        OnboardingDocument saved;
        try {
            saved = documentRepository.save(OnboardingDocument.builder()
                    .application(application)
                    .documentType(documentType)
                    .originalFileName(cleanFileName(file.getOriginalFilename()))
                    .declaredContentType(cleanContentType(file.getContentType()))
                    .storageProvider(storage.provider())
                    .storageBucket(storage.quarantineBucket())
                    .storageKey(key)
                    .uploadedByUserId(applicantUserId)
                    .build());
        } catch (RuntimeException e) {
            removeQuarantinedQuietly(key);
            throw e;
        }

        // A draft is the applicant's private working copy; staff only see uploads made while the case is in review.
        if (application.getStatus() != OnboardingStatus.DRAFT) {
            support.record(application, applicantUserId, ReviewDecision.DOCUMENT_UPLOADED,
                    saved.getOriginalFileName() + " (" + documentType + ")", null, null);
        }
        eventPublisher.publishEvent(DocumentMapper.toUploadedEvent(saved));
        return DocumentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getMyDocuments(Long applicationId, Long applicantUserId) {
        support.requireOwnedByApplicant(support.getOrThrow(applicationId), applicantUserId);
        return DocumentMapper.toResponseList(documentRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId));
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadUrlResponse getMyDownloadUrl(Long applicationId, Long documentId, Long applicantUserId) {
        support.requireOwnedByApplicant(support.getOrThrow(applicationId), applicantUserId);
        return downloadUrl(requireDocument(applicationId, documentId));
    }

    @Override
    @Transactional
    public void delete(Long applicationId, Long documentId, Long applicantUserId) {
        AirlineOnboardingApplication application = support.getOrThrow(applicationId);
        support.requireOwnedByApplicant(application, applicantUserId);
        OnboardingDocument document = requireDocument(applicationId, documentId);
        if (application.getStatus() != OnboardingStatus.DRAFT) {
            throw new ConflictException(String.format(ErrorMessageUtil.DOCUMENT_NOT_DELETABLE, documentId));
        }
        documentRepository.delete(document);
        removeStoredQuietly(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsForReview(Long applicationId) {
        support.getVisibleOrThrow(applicationId);
        return DocumentMapper.toResponseList(documentRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .filter(document -> VIEWABLE.contains(document.getStatus()))
                .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadUrlResponse getDownloadUrlForReview(Long applicationId, Long documentId) {
        support.getVisibleOrThrow(applicationId);
        return downloadUrl(requireDocument(applicationId, documentId));
    }

    @Override
    @Transactional
    public DocumentResponse verify(Long applicationId, Long documentId, Long actorUserId) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, Set.of(OnboardingStatus.UNDER_REVIEW), ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_UNDER_REVIEW);
        support.requireNotApplicantOrNominee(application, actorUserId);
        OnboardingDocument document = requireDocument(applicationId, documentId);
        if (document.getStatus() != DocumentStatus.CLEAN) {
            throw new ConflictException(String.format(ErrorMessageUtil.DOCUMENT_NOT_CLEAN, documentId, document.getStatus()));
        }

        document.setStatus(DocumentStatus.VERIFIED);
        document.setVerifiedByUserId(actorUserId);
        document.setVerifiedAt(Instant.now());
        document.setRejectionReason(null);
        OnboardingDocument saved = documentRepository.save(document);
        support.record(application, actorUserId, ReviewDecision.DOCUMENT_VERIFIED,
                saved.getOriginalFileName() + " (" + saved.getDocumentType() + ")", null, OnboardingStage.COMPLIANCE);
        return DocumentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DocumentResponse reject(Long applicationId, Long documentId, Long actorUserId, String reason) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, Set.of(OnboardingStatus.UNDER_REVIEW), ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_UNDER_REVIEW);
        support.requireNotApplicantOrNominee(application, actorUserId);
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException(ErrorMessageUtil.DOCUMENT_REJECTION_REASON_MANDATORY);
        }
        OnboardingDocument document = requireDocument(applicationId, documentId);
        if (document.getStatus() != DocumentStatus.CLEAN && document.getStatus() != DocumentStatus.VERIFIED) {
            throw new ConflictException(String.format(ErrorMessageUtil.DOCUMENT_NOT_CLEAN, documentId, document.getStatus()));
        }

        document.setStatus(DocumentStatus.REJECTED);
        document.setRejectionReason(reason);
        document.setVerifiedByUserId(null);
        document.setVerifiedAt(null);
        OnboardingDocument saved = documentRepository.save(document);
        support.record(application, actorUserId, ReviewDecision.DOCUMENT_REJECTED,
                saved.getOriginalFileName() + " (" + saved.getDocumentType() + "): " + reason, null, OnboardingStage.COMPLIANCE);
        return DocumentMapper.toResponse(saved);
    }

    private OnboardingDocument requireDocument(Long applicationId, Long documentId) {
        return documentRepository.findByIdAndApplicationId(documentId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.DOCUMENT_NOT_FOUND_BY_ID, documentId)));
    }

    /** Only files that passed the automatic checks can be viewed: never one still being inspected, and never a blocked one. */
    private DownloadUrlResponse downloadUrl(OnboardingDocument document) {
        if (!VIEWABLE.contains(document.getStatus())) {
            throw new ConflictException(String.format(ErrorMessageUtil.DOCUMENT_NOT_AVAILABLE, document.getId(), document.getStatus()));
        }
        DocumentStorage.PresignedDownload link = storage.presignDownload(
                document.getStorageKey(), document.getOriginalFileName(), document.getDetectedContentType());
        return DownloadUrlResponse.builder()
                .url(link.url().toString())
                .expiresAt(link.expiresAt())
                .fileName(document.getOriginalFileName())
                .contentType(document.getDetectedContentType())
                .build();
    }

    private void removeStoredQuietly(OnboardingDocument document) {
        try {
            if (document.getStorageBucket().equals(storage.quarantineBucket())) {
                storage.deleteQuarantine(document.getStorageKey());
            } else {
                storage.deleteClean(document.getStorageKey());
            }
        } catch (RuntimeException e) {
            log.warn("Could not remove stored file of deleted document {}", document.getId(), e);
        }
    }

    /** Uploads are open while the application is a draft, or in review while a question to the applicant is open. */
    private void requireUploadsAccepted(AirlineOnboardingApplication application) {
        OnboardingStatus status = application.getStatus();
        if (status == OnboardingStatus.DRAFT) {
            return;
        }
        if (status == OnboardingStatus.UNDER_REVIEW
                && informationRequestRepository.existsByApplicationIdAndStatus(application.getId(), InformationRequestStatus.OPEN)) {
            return;
        }
        throw new ConflictException(String.format(ErrorMessageUtil.DOCUMENT_UPLOAD_NOT_ALLOWED, application.getId(), status));
    }

    private void requireAcceptableFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(ErrorMessageUtil.DOCUMENT_EMPTY);
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BadRequestException(String.format(ErrorMessageUtil.DOCUMENT_TOO_LARGE, properties.getMaxFileSizeBytes()));
        }
    }

    private void removeQuarantinedQuietly(String key) {
        try {
            storage.deleteQuarantine(key);
        } catch (RuntimeException e) {
            log.warn("Could not remove quarantined object {} after a failed save", key, e);
        }
    }

    /** A name safe to store and show: no path, no control characters, no odd symbols, never blank, never hidden. */
    static String cleanFileName(String original) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("\\p{Cntrl}", "");
        name = name.replaceAll("[^\\p{L}\\p{N}._ ()\\-]", "_");
        name = name.replaceAll("^\\.+", "").strip();
        if (name.isEmpty()) {
            return "document";
        }
        return name.length() > MAX_FILE_NAME_LENGTH ? name.substring(name.length() - MAX_FILE_NAME_LENGTH) : name;
    }

    private static String cleanContentType(String declared) {
        if (declared == null) {
            return null;
        }
        String cleaned = declared.replaceAll("\\p{Cntrl}", "").strip();
        if (cleaned.isEmpty()) {
            return null;
        }
        return cleaned.length() > MAX_CONTENT_TYPE_LENGTH ? cleaned.substring(0, MAX_CONTENT_TYPE_LENGTH) : cleaned;
    }
}
