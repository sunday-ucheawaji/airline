package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ConflictException;
import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.enums.DocumentType;
import com.sunday.services.model.OnboardingDocument;
import com.sunday.services.repository.OnboardingDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingDocumentGateTest {

    private static final long APPLICATION = 10L;
    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Mock OnboardingDocumentRepository documentRepository;

    private OnboardingDocumentGate gate;
    private final List<OnboardingDocument> documents = new ArrayList<>();
    private long nextId = 1;

    @BeforeEach
    void setUp() {
        gate = new OnboardingDocumentGate(documentRepository);
        when(documentRepository.findByApplicationIdOrderByCreatedAtAsc(APPLICATION)).thenReturn(documents);
    }

    @Test
    void submissionNeedsBothRequiredDocumentTypes() {
        add(DocumentType.CERTIFICATE_OF_INCORPORATION, DocumentStatus.CLEAN, 1);

        assertThatThrownBy(() -> gate.requireReadyForSubmission(APPLICATION))
                .isInstanceOf(ConflictException.class).hasMessageContaining("AIR_OPERATOR_CERTIFICATE")
                .hasMessageNotContaining("CERTIFICATE_OF_INCORPORATION");
    }

    @Test
    void nothingUploadedNamesEveryRequiredType() {
        assertThatThrownBy(() -> gate.requireReadyForSubmission(APPLICATION))
                .hasMessageContaining("CERTIFICATE_OF_INCORPORATION").hasMessageContaining("AIR_OPERATOR_CERTIFICATE");
    }

    @Test
    void optionalDocumentTypesAreNotRequired() {
        add(DocumentType.CERTIFICATE_OF_INCORPORATION, DocumentStatus.CLEAN, 1);
        add(DocumentType.AIR_OPERATOR_CERTIFICATE, DocumentStatus.CLEAN, 2);

        assertThatCode(() -> gate.requireReadyForSubmission(APPLICATION)).doesNotThrowAnyException();
    }

    @Test
    void adocumentStillBeingCheckedOrBlockedOrRejectedDoesNotCountForSubmission() {
        add(DocumentType.CERTIFICATE_OF_INCORPORATION, DocumentStatus.PROCESSING, 1);
        add(DocumentType.AIR_OPERATOR_CERTIFICATE, DocumentStatus.BLOCKED, 2);
        assertThatThrownBy(() -> gate.requireReadyForSubmission(APPLICATION))
                .hasMessageContaining("CERTIFICATE_OF_INCORPORATION").hasMessageContaining("AIR_OPERATOR_CERTIFICATE");

        documents.clear();
        add(DocumentType.CERTIFICATE_OF_INCORPORATION, DocumentStatus.REJECTED, 1);
        add(DocumentType.AIR_OPERATOR_CERTIFICATE, DocumentStatus.CLEAN, 2);
        assertThatThrownBy(() -> gate.requireReadyForSubmission(APPLICATION)).hasMessageContaining("CERTIFICATE_OF_INCORPORATION");
    }

    @Test
    void onlyTheNewestUploadOfATypeCounts() {
        add(DocumentType.CERTIFICATE_OF_INCORPORATION, DocumentStatus.VERIFIED, 1);
        add(DocumentType.AIR_OPERATOR_CERTIFICATE, DocumentStatus.VERIFIED, 2);
        assertThatCode(() -> gate.requireVerifiedForCompliance(APPLICATION)).doesNotThrowAnyException();

        add(DocumentType.AIR_OPERATOR_CERTIFICATE, DocumentStatus.CLEAN, 3);

        assertThatThrownBy(() -> gate.requireVerifiedForCompliance(APPLICATION))
                .isInstanceOf(ConflictException.class).hasMessageContaining("AIR_OPERATOR_CERTIFICATE")
                .hasMessageNotContaining("CERTIFICATE_OF_INCORPORATION");
    }

    @Test
    void aNewerBlockedUploadDoesNotHideThatTheOlderOneIsNoLongerTheCurrentOne() {
        add(DocumentType.CERTIFICATE_OF_INCORPORATION, DocumentStatus.CLEAN, 1);
        add(DocumentType.AIR_OPERATOR_CERTIFICATE, DocumentStatus.CLEAN, 2);
        add(DocumentType.AIR_OPERATOR_CERTIFICATE, DocumentStatus.BLOCKED, 3);

        assertThatThrownBy(() -> gate.requireReadyForSubmission(APPLICATION)).hasMessageContaining("AIR_OPERATOR_CERTIFICATE");
    }

    @Test
    void complianceApprovalNeedsEveryRequiredDocumentVerified() {
        add(DocumentType.CERTIFICATE_OF_INCORPORATION, DocumentStatus.VERIFIED, 1);
        add(DocumentType.AIR_OPERATOR_CERTIFICATE, DocumentStatus.CLEAN, 2);
        assertThatThrownBy(() -> gate.requireVerifiedForCompliance(APPLICATION)).isInstanceOf(ConflictException.class);

        documents.get(1).setStatus(DocumentStatus.VERIFIED);
        assertThatCode(() -> gate.requireVerifiedForCompliance(APPLICATION)).doesNotThrowAnyException();
    }

    @Test
    void whenTwoUploadsShareATimestampTheOneSavedLaterWins() {
        add(DocumentType.CERTIFICATE_OF_INCORPORATION, DocumentStatus.CLEAN, 5);
        add(DocumentType.CERTIFICATE_OF_INCORPORATION, DocumentStatus.VERIFIED, 5);
        add(DocumentType.AIR_OPERATOR_CERTIFICATE, DocumentStatus.VERIFIED, 1);

        assertThatCode(() -> gate.requireVerifiedForCompliance(APPLICATION)).doesNotThrowAnyException();
    }

    private void add(DocumentType type, DocumentStatus status, int secondsAfterStart) {
        documents.add(OnboardingDocument.builder().id(nextId++).documentType(type).status(status)
                .createdAt(T0.plusSeconds(secondsAfterStart)).build());
    }
}
