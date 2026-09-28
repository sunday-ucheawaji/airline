package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.ReviewDecision;
import com.sunday.common_lib.event.OnboardingDocumentUploadedEvent;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.DocumentResponse;
import com.sunday.common_lib.payload.response.DownloadUrlResponse;
import com.sunday.services.config.DocumentProperties;
import com.sunday.services.enums.DocumentStatus;
import com.sunday.services.enums.DocumentType;
import com.sunday.services.enums.InformationRequestStatus;
import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingDocument;
import com.sunday.services.repository.AirlineOnboardingApplicationRepository;
import com.sunday.services.repository.OnboardingCaseOwnerHistoryRepository;
import com.sunday.services.repository.OnboardingDocumentRepository;
import com.sunday.services.repository.OnboardingInformationRequestRepository;
import com.sunday.services.repository.OnboardingReviewRepository;
import com.sunday.services.storage.InMemoryDocumentStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnboardingDocumentServiceImplTest {

    private static final long APPLICATION = 10L;
    private static final long APPLICANT = 1L;

    @Mock AirlineOnboardingApplicationRepository applicationRepository;
    @Mock OnboardingReviewRepository reviewRepository;
    @Mock OnboardingCaseOwnerHistoryRepository caseOwnerHistoryRepository;
    @Mock OnboardingInformationRequestRepository informationRequestRepository;
    @Mock OnboardingDocumentRepository documentRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    private final InMemoryDocumentStorage storage = new InMemoryDocumentStorage();
    private final DocumentProperties properties = new DocumentProperties();
    private OnboardingDocumentServiceImpl service;
    private AirlineOnboardingApplication application;

    @BeforeEach
    void setUp() {
        OnboardingSupport support = new OnboardingSupport(
                applicationRepository, reviewRepository, caseOwnerHistoryRepository, informationRequestRepository);
        service = new OnboardingDocumentServiceImpl(support, documentRepository, informationRequestRepository,
                storage, properties, eventPublisher);

        application = new AirlineOnboardingApplication();
        application.setId(APPLICATION);
        application.setApplicantUserId(APPLICANT);
        application.setStatus(OnboardingStatus.DRAFT);
        when(applicationRepository.findById(APPLICATION)).thenReturn(Optional.of(application));
        when(documentRepository.save(any(OnboardingDocument.class))).thenAnswer(i -> {
            OnboardingDocument d = i.getArgument(0);
            d.setId(77L);
            d.setCreatedAt(Instant.now());
            return d;
        });
        when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ---------------------------------------------------------------- the happy path

    @Test
    void anUploadGoesToQuarantineIsRecordedAsProcessingAndAnEventIsPublished() {
        byte[] bytes = "%PDF-1.7 content".getBytes();

        DocumentResponse response = service.upload(APPLICATION, APPLICANT, DocumentType.CERTIFICATE_OF_INCORPORATION,
                new MockMultipartFile("file", "certificate.pdf", "application/pdf", bytes));

        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        assertThat(response.getDocumentType()).isEqualTo("CERTIFICATE_OF_INCORPORATION");
        assertThat(response.getDetectedContentType()).isNull();
        assertThat(storage.quarantine).hasSize(1);
        assertThat(storage.quarantine.values()).containsExactly(bytes);
        assertThat(storage.clean).isEmpty();

        ArgumentCaptor<OnboardingDocument> saved = ArgumentCaptor.forClass(OnboardingDocument.class);
        verify(documentRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(DocumentStatus.PROCESSING);
        assertThat(saved.getValue().getStorageBucket()).isEqualTo("quarantine");

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        OnboardingDocumentUploadedEvent published = (OnboardingDocumentUploadedEvent) event.getValue();
        assertThat(published.getDocumentId()).isEqualTo(77L);
        assertThat(published.getApplicationId()).isEqualTo(APPLICATION);
        assertThat(published.getQuarantineKey()).isEqualTo(storage.quarantine.keySet().iterator().next());
        assertThat(published.getDeclaredContentType()).isEqualTo("application/pdf");
    }

    @Test
    void theStorageKeyIsGeneratedAndNeverContainsWhatTheUploaderSent() {
        service.upload(APPLICATION, APPLICANT, DocumentType.OTHER,
                new MockMultipartFile("file", "../../etc/passwd.pdf", "application/pdf", new byte[]{1, 2, 3}));

        String key = storage.quarantine.keySet().iterator().next();
        assertThat(key).startsWith("onboarding/" + APPLICATION + "/").doesNotContain("passwd", "..", ".pdf");
    }

    @Test
    void theDeclaredContentTypeIsOnlyRecordedNotTrusted() {
        DocumentResponse response = service.upload(APPLICATION, APPLICANT, DocumentType.OTHER,
                new MockMultipartFile("file", "malware.exe", "application/pdf", new byte[]{'M', 'Z'}));

        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        assertThat(response.getDetectedContentType()).isNull();
    }

    // ---------------------------------------------------------------- file names

    @Test
    void fileNamesAreCleanedForStorageAndDisplay() {
        assertThat(OnboardingDocumentServiceImpl.cleanFileName("licence.pdf")).isEqualTo("licence.pdf");
        assertThat(OnboardingDocumentServiceImpl.cleanFileName("C:\\Users\\me\\..\\licence.pdf")).isEqualTo("licence.pdf");
        assertThat(OnboardingDocumentServiceImpl.cleanFileName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(OnboardingDocumentServiceImpl.cleanFileName("a\r\nb<script>.pdf")).doesNotContain("\r", "\n", "<", ">");
        assertThat(OnboardingDocumentServiceImpl.cleanFileName("...hidden")).isEqualTo("hidden");
        assertThat(OnboardingDocumentServiceImpl.cleanFileName(null)).isEqualTo("document");
        assertThat(OnboardingDocumentServiceImpl.cleanFileName("///")).isEqualTo("document");
        assertThat(OnboardingDocumentServiceImpl.cleanFileName("x".repeat(400))).hasSize(255);
    }

    // ---------------------------------------------------------------- who and when

    @Test
    void onlyTheOwnerOfTheApplicationCanUpload() {
        assertThatThrownBy(() -> service.upload(APPLICATION, 999L, DocumentType.OTHER, pdf()))
                .isInstanceOf(OperationNotPermittedException.class);
        assertThat(storage.quarantine).isEmpty();
    }

    @Test
    void aSubmittedApplicationDoesNotAcceptUploads() {
        application.setStatus(OnboardingStatus.SUBMITTED);

        assertThatThrownBy(() -> service.upload(APPLICATION, APPLICANT, DocumentType.OTHER, pdf())).isInstanceOf(ConflictException.class);
        assertThat(storage.quarantine).isEmpty();
    }

    @Test
    void anApplicationInReviewAcceptsUploadsOnlyWhileAnInformationRequestIsOpen() {
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        when(informationRequestRepository.existsByApplicationIdAndStatus(APPLICATION, InformationRequestStatus.OPEN)).thenReturn(false);
        assertThatThrownBy(() -> service.upload(APPLICATION, APPLICANT, DocumentType.OTHER, pdf())).isInstanceOf(ConflictException.class);

        when(informationRequestRepository.existsByApplicationIdAndStatus(APPLICATION, InformationRequestStatus.OPEN)).thenReturn(true);
        assertThat(service.upload(APPLICATION, APPLICANT, DocumentType.OTHER, pdf()).getStatus()).isEqualTo("PROCESSING");
    }

    @Test
    void anUploadDuringReviewIsShownToStaffInTheTimelineButADraftUploadIsNot() {
        service.upload(APPLICATION, APPLICANT, DocumentType.OTHER, pdf());
        verify(reviewRepository, never()).save(any());

        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        when(informationRequestRepository.existsByApplicationIdAndStatus(APPLICATION, InformationRequestStatus.OPEN)).thenReturn(true);
        service.upload(APPLICATION, APPLICANT, DocumentType.OTHER, pdf());

        ArgumentCaptor<com.sunday.services.model.OnboardingReview> entry = ArgumentCaptor.forClass(com.sunday.services.model.OnboardingReview.class);
        verify(reviewRepository).save(entry.capture());
        assertThat(entry.getValue().getDecision()).isEqualTo(ReviewDecision.DOCUMENT_UPLOADED);
        assertThat(entry.getValue().getActorUserId()).isEqualTo(APPLICANT);
    }

    // ---------------------------------------------------------------- limits

    @Test
    void anEmptyOrOversizedFileIsRefusedBeforeItTouchesStorage() {
        assertThatThrownBy(() -> service.upload(APPLICATION, APPLICANT, DocumentType.OTHER,
                new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]))).isInstanceOf(BadRequestException.class);

        properties.setMaxFileSizeBytes(10);
        assertThatThrownBy(() -> service.upload(APPLICATION, APPLICANT, DocumentType.OTHER,
                new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[11]))).isInstanceOf(BadRequestException.class);
        assertThat(storage.quarantine).isEmpty();
    }

    @Test
    void anApplicationCannotExceedItsDocumentQuota() {
        properties.setMaxDocumentsPerApplication(2);
        when(documentRepository.countByApplicationId(APPLICATION)).thenReturn(2L);

        assertThatThrownBy(() -> service.upload(APPLICATION, APPLICANT, DocumentType.OTHER, pdf())).isInstanceOf(ConflictException.class);
        assertThat(storage.quarantine).isEmpty();
    }

    // ---------------------------------------------------------------- failures

    @Test
    void aStorageOutageFailsClosedWithNoRecordAndNoEvent() {
        storage.unavailable = true;

        assertThatThrownBy(() -> service.upload(APPLICATION, APPLICANT, DocumentType.OTHER, pdf()))
                .isInstanceOf(ServiceUnavailableException.class);
        verify(documentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void ifSavingTheRecordFailsTheQuarantinedFileIsRemoved() {
        when(documentRepository.save(any(OnboardingDocument.class))).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> service.upload(APPLICATION, APPLICANT, DocumentType.OTHER, pdf()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(storage.quarantine).isEmpty();
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    // ---------------------------------------------------------------- listing

    @Test
    void anApplicantSeesTheirOwnDocumentsWithTheirStatus() {
        OnboardingDocument processing = OnboardingDocument.builder().id(1L).application(application)
                .documentType(DocumentType.OTHER).originalFileName("a.pdf").storageProvider("MEMORY").storageBucket("quarantine")
                .storageKey("k1").uploadedByUserId(APPLICANT).build();
        OnboardingDocument blocked = OnboardingDocument.builder().id(2L).application(application)
                .documentType(DocumentType.OTHER).originalFileName("b.pdf").storageProvider("MEMORY").storageBucket("quarantine")
                .storageKey("k2").uploadedByUserId(APPLICANT).status(DocumentStatus.BLOCKED).blockedReason("unsafe").build();
        when(documentRepository.findByApplicationIdOrderByCreatedAtAsc(APPLICATION)).thenReturn(List.of(processing, blocked));

        List<DocumentResponse> mine = service.getMyDocuments(APPLICATION, APPLICANT);

        assertThat(mine).extracting(DocumentResponse::getStatus).containsExactly("PROCESSING", "BLOCKED");
        assertThat(mine.get(1).getBlockedReason()).isEqualTo("unsafe");
        assertThatThrownBy(() -> service.getMyDocuments(APPLICATION, 999L)).isInstanceOf(OperationNotPermittedException.class);
    }

    // ---------------------------------------------------------------- download links

    @Test
    void anApplicantGetsASignedLinkToTheirOwnCleanDocument() {
        stored(5L, DocumentStatus.CLEAN, "documents/10/abc", "clean");

        DownloadUrlResponse link = service.getMyDownloadUrl(APPLICATION, 5L, APPLICANT);

        assertThat(link.getUrl()).startsWith("http://storage.test/documents/10/abc");
        assertThat(link.getContentType()).isEqualTo("application/pdf");
        assertThat(link.getFileName()).isEqualTo("certificate.pdf");
        assertThat(link.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void aDocumentStillBeingCheckedOrBlockedHasNoDownloadLink() {
        stored(5L, DocumentStatus.PROCESSING, "onboarding/10/q", "quarantine");
        assertThatThrownBy(() -> service.getMyDownloadUrl(APPLICATION, 5L, APPLICANT)).isInstanceOf(ConflictException.class);

        stored(6L, DocumentStatus.BLOCKED, "onboarding/10/b", "quarantine");
        assertThatThrownBy(() -> service.getMyDownloadUrl(APPLICATION, 6L, APPLICANT)).isInstanceOf(ConflictException.class);
    }

    @Test
    void aDownloadLinkNeedsTheOwnerAndADocumentOfThatApplication() {
        stored(5L, DocumentStatus.CLEAN, "documents/10/abc", "clean");

        assertThatThrownBy(() -> service.getMyDownloadUrl(APPLICATION, 5L, 999L)).isInstanceOf(OperationNotPermittedException.class);
        assertThatThrownBy(() -> service.getMyDownloadUrl(APPLICATION, 404L, APPLICANT)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------- deleting

    @Test
    void anApplicantCanDeleteAnUploadWhileTheApplicationIsADraftAndItsFileGoesToo() {
        stored(5L, DocumentStatus.CLEAN, "documents/10/abc", "clean");
        storage.clean.put("documents/10/abc", new byte[]{1});
        stored(6L, DocumentStatus.PROCESSING, "onboarding/10/q", "quarantine");
        storage.quarantine.put("onboarding/10/q", new byte[]{2});

        service.delete(APPLICATION, 5L, APPLICANT);
        service.delete(APPLICATION, 6L, APPLICANT);

        verify(documentRepository, org.mockito.Mockito.times(2)).delete(any(OnboardingDocument.class));
        assertThat(storage.clean).isEmpty();
        assertThat(storage.quarantine).isEmpty();
    }

    @Test
    void nothingCanBeDeletedOnceTheApplicationIsSubmittedOrByAnotherUser() {
        stored(5L, DocumentStatus.CLEAN, "documents/10/abc", "clean");
        assertThatThrownBy(() -> service.delete(APPLICATION, 5L, 999L)).isInstanceOf(OperationNotPermittedException.class);

        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        assertThatThrownBy(() -> service.delete(APPLICATION, 5L, APPLICANT)).isInstanceOf(ConflictException.class);
        verify(documentRepository, never()).delete(any(OnboardingDocument.class));
    }

    // ---------------------------------------------------------------- staff view

    @Test
    void staffOnlySeeDocumentsThatPassedTheChecks() {
        OnboardingDocument processing = doc(1L, DocumentStatus.PROCESSING, "k1", "quarantine");
        OnboardingDocument blocked = doc(2L, DocumentStatus.BLOCKED, "k2", "quarantine");
        OnboardingDocument clean = doc(3L, DocumentStatus.CLEAN, "k3", "clean");
        OnboardingDocument verified = doc(4L, DocumentStatus.VERIFIED, "k4", "clean");
        OnboardingDocument rejected = doc(5L, DocumentStatus.REJECTED, "k5", "clean");
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        when(documentRepository.findByApplicationIdOrderByCreatedAtAsc(APPLICATION))
                .thenReturn(List.of(processing, blocked, clean, verified, rejected));

        assertThat(service.getDocumentsForReview(APPLICATION)).extracting(DocumentResponse::getId).containsExactly(3L, 4L, 5L);
    }

    @Test
    void staffCannotSeeTheDocumentsOfADraft() {
        assertThatThrownBy(() -> service.getDocumentsForReview(APPLICATION)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getDownloadUrlForReview(APPLICATION, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void staffGetASignedLinkOnlyForDocumentsThatPassedTheChecks() {
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        stored(5L, DocumentStatus.CLEAN, "documents/10/abc", "clean");
        assertThat(service.getDownloadUrlForReview(APPLICATION, 5L).getUrl()).contains("documents/10/abc");

        stored(6L, DocumentStatus.BLOCKED, "onboarding/10/b", "quarantine");
        assertThatThrownBy(() -> service.getDownloadUrlForReview(APPLICATION, 6L)).isInstanceOf(ConflictException.class);
    }

    // ---------------------------------------------------------------- verifying and rejecting

    @Test
    void aReviewerVerifiesACleanDocumentAndTheTimelineRecordsIt() {
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        OnboardingDocument document = stored(5L, DocumentStatus.CLEAN, "documents/10/abc", "clean");

        DocumentResponse response = service.verify(APPLICATION, 5L, 30L);

        assertThat(response.getStatus()).isEqualTo("VERIFIED");
        assertThat(document.getVerifiedByUserId()).isEqualTo(30L);
        assertThat(document.getVerifiedAt()).isNotNull();
        ArgumentCaptor<com.sunday.services.model.OnboardingReview> entry = ArgumentCaptor.forClass(com.sunday.services.model.OnboardingReview.class);
        verify(reviewRepository).save(entry.capture());
        assertThat(entry.getValue().getDecision()).isEqualTo(ReviewDecision.DOCUMENT_VERIFIED);
        assertThat(entry.getValue().getStage()).isEqualTo(OnboardingStage.COMPLIANCE);
        assertThat(entry.getValue().getActorUserId()).isEqualTo(30L);
    }

    @Test
    void onlyACleanDocumentCanBeVerifiedAndOnlyDuringReview() {
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        stored(5L, DocumentStatus.PROCESSING, "onboarding/10/q", "quarantine");
        stored(6L, DocumentStatus.REJECTED, "documents/10/r", "clean");
        assertThatThrownBy(() -> service.verify(APPLICATION, 5L, 30L)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.verify(APPLICATION, 6L, 30L)).isInstanceOf(ConflictException.class);

        application.setStatus(OnboardingStatus.SUBMITTED);
        stored(7L, DocumentStatus.CLEAN, "documents/10/c", "clean");
        assertThatThrownBy(() -> service.verify(APPLICATION, 7L, 30L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void theApplicantAndTheNomineeCannotVerifyOrRejectDocuments() {
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        application.setInitialAdminUserId(2L);
        stored(5L, DocumentStatus.CLEAN, "documents/10/abc", "clean");

        assertThatThrownBy(() -> service.verify(APPLICATION, 5L, APPLICANT)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.verify(APPLICATION, 5L, 2L)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.reject(APPLICATION, 5L, APPLICANT, "no")).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectingNeedsAReasonAndRecordsIt() {
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        OnboardingDocument document = stored(5L, DocumentStatus.CLEAN, "documents/10/abc", "clean");

        assertThatThrownBy(() -> service.reject(APPLICATION, 5L, 30L, " ")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.reject(APPLICATION, 5L, 30L, null)).isInstanceOf(BadRequestException.class);

        DocumentResponse response = service.reject(APPLICATION, 5L, 30L, "licence has expired");

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(document.getRejectionReason()).isEqualTo("licence has expired");
    }

    @Test
    void aVerifiedDocumentCanStillBeRejectedAndLosesItsVerification() {
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        OnboardingDocument document = stored(5L, DocumentStatus.VERIFIED, "documents/10/abc", "clean");
        document.setVerifiedByUserId(31L);
        document.setVerifiedAt(Instant.now());

        service.reject(APPLICATION, 5L, 30L, "found a discrepancy");

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.REJECTED);
        assertThat(document.getVerifiedByUserId()).isNull();
        assertThat(document.getVerifiedAt()).isNull();
    }

    @Test
    void aDocumentThatIsStillBeingCheckedCannotBeRejected() {
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        stored(5L, DocumentStatus.PROCESSING, "onboarding/10/q", "quarantine");

        assertThatThrownBy(() -> service.reject(APPLICATION, 5L, 30L, "why")).isInstanceOf(ConflictException.class);
    }

    private OnboardingDocument stored(Long id, DocumentStatus status, String key, String bucket) {
        OnboardingDocument document = doc(id, status, key, bucket);
        when(documentRepository.findByIdAndApplicationId(id, APPLICATION)).thenReturn(Optional.of(document));
        return document;
    }

    private OnboardingDocument doc(Long id, DocumentStatus status, String key, String bucket) {
        return OnboardingDocument.builder().id(id).application(application).documentType(DocumentType.CERTIFICATE_OF_INCORPORATION)
                .originalFileName("certificate.pdf").declaredContentType("application/pdf").detectedContentType("application/pdf")
                .storageProvider("MEMORY").storageBucket(bucket).storageKey(key).uploadedByUserId(APPLICANT).status(status).build();
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "doc.pdf", "application/pdf", "%PDF-1.7".getBytes());
    }
}
