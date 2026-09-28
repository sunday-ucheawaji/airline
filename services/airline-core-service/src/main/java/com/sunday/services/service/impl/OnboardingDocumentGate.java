package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.enums.DocumentType;
import com.sunday.services.model.OnboardingDocument;
import com.sunday.services.repository.OnboardingDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The document rules that gate the onboarding flow. For each required document type only the newest upload counts,
 * so a replacement always supersedes what it replaces: a fresh upload that is still being checked, was blocked, or
 * was rejected does not satisfy the requirement, however good the older one was.
 */
@Component
@RequiredArgsConstructor
public class OnboardingDocumentGate {

    private static final Set<DocumentStatus> READY = EnumSet.of(DocumentStatus.CLEAN, DocumentStatus.VERIFIED);
    private static final Set<DocumentStatus> VERIFIED = EnumSet.of(DocumentStatus.VERIFIED);

    private final OnboardingDocumentRepository documentRepository;

    /** To submit, every required type needs a newest upload that has passed the automatic checks. */
    public void requireReadyForSubmission(Long applicationId) {
        List<DocumentType> unsatisfied = unsatisfied(applicationId, READY);
        if (!unsatisfied.isEmpty()) {
            throw new ConflictException(String.format(ErrorMessageUtil.DOCUMENT_REQUIRED_MISSING, names(unsatisfied)));
        }
    }

    /** The compliance stage can only be approved once every required document has been verified by a reviewer. */
    public void requireVerifiedForCompliance(Long applicationId) {
        List<DocumentType> unsatisfied = unsatisfied(applicationId, VERIFIED);
        if (!unsatisfied.isEmpty()) {
            throw new ConflictException(String.format(ErrorMessageUtil.DOCUMENT_REQUIRED_NOT_VERIFIED, names(unsatisfied)));
        }
    }

    private List<DocumentType> unsatisfied(Long applicationId, Set<DocumentStatus> acceptable) {
        Map<DocumentType, OnboardingDocument> newest = new HashMap<>();
        documentRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .sorted(Comparator.comparing(OnboardingDocument::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(OnboardingDocument::getId))
                .forEach(document -> newest.put(document.getDocumentType(), document));
        return DocumentType.requiredTypes().stream()
                .filter(type -> !newest.containsKey(type) || !acceptable.contains(newest.get(type).getStatus()))
                .toList();
    }

    private static String names(List<DocumentType> types) {
        return types.stream().map(DocumentType::name).collect(Collectors.joining(", "));
    }
}
