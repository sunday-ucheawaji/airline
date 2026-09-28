package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.enums.StageStatus;
import com.sunday.services.model.Airline;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingStageReview;
import com.sunday.services.repository.AirlineMembershipRepository;
import com.sunday.services.repository.AirlineOnboardingApplicationRepository;
import com.sunday.services.repository.AirlineRepository;
import com.sunday.services.repository.OnboardingCaseOwnerHistoryRepository;
import com.sunday.services.repository.OnboardingInformationRequestRepository;
import com.sunday.services.repository.OnboardingReviewRepository;
import com.sunday.services.repository.OnboardingStageReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnboardingServiceImplTest {

    private static final long APPLICATION = 10L;
    private static final long APPLICANT = 1L;
    private static final long NOMINEE = 2L;
    private static final long OWNER = 20L;
    private static final long APPROVER = 40L;
    private static final long PROVISIONER = 50L;

    @Mock AirlineOnboardingApplicationRepository applicationRepository;
    @Mock OnboardingReviewRepository reviewRepository;
    @Mock OnboardingStageReviewRepository stageRepository;
    @Mock OnboardingCaseOwnerHistoryRepository caseOwnerHistoryRepository;
    @Mock OnboardingInformationRequestRepository informationRequestRepository;
    @Mock AirlineRepository airlineRepository;
    @Mock AirlineMembershipRepository airlineMembershipRepository;
    @Mock PlatformUserGuard platformUserGuard;
    @Mock OnboardingDocumentGate documentGate;

    private OnboardingServiceImpl service;
    private AirlineOnboardingApplication application;

    @BeforeEach
    void setUp() {
        OnboardingSupport support = new OnboardingSupport(
                applicationRepository, reviewRepository, caseOwnerHistoryRepository, informationRequestRepository);
        service = new OnboardingServiceImpl(applicationRepository, reviewRepository, stageRepository, airlineRepository,
                airlineMembershipRepository, platformUserGuard, support, documentGate);
        ReflectionTestUtils.setField(service, "ownerRoleId", 3L);

        application = new AirlineOnboardingApplication();
        application.setId(APPLICATION);
        application.setApplicantUserId(APPLICANT);
        application.setInitialAdminUserId(NOMINEE);
        application.setCaseOwnerUserId(OWNER);
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        application.setLegalName("E2E Air Ltd");
        application.setDisplayName("E2E Air");
        application.setCountry("Nigeria");
        application.setRegistrationNumber("RC1");

        when(applicationRepository.findById(APPLICATION)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(AirlineOnboardingApplication.class))).thenAnswer(i -> i.getArgument(0));
        when(stageRepository.save(any(OnboardingStageReview.class))).thenAnswer(i -> i.getArgument(0));
        when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(airlineRepository.save(any(Airline.class))).thenAnswer(i -> {
            Airline airline = i.getArgument(0);
            airline.setId(7L);
            return airline;
        });
    }

    // ---------------------------------------------------------------- submission starts a review round

    @Test
    void submittingCreatesThreePendingUnassignedStages() {
        application.setStatus(OnboardingStatus.DRAFT);
        application.setCaseOwnerUserId(null);
        when(stageRepository.findByApplicationIdAndStage(any(), any())).thenReturn(Optional.empty());

        OnboardingApplicationResponse response = service.submitApplication(APPLICATION, APPLICANT);

        assertThat(response.getStatus()).isEqualTo("SUBMITTED");
        ArgumentCaptor<OnboardingStageReview> captor = ArgumentCaptor.forClass(OnboardingStageReview.class);
        verify(stageRepository, times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(OnboardingStageReview::getStage)
                .containsExactlyInAnyOrder(OnboardingStage.values());
        assertThat(captor.getAllValues()).allSatisfy(review -> {
            assertThat(review.getStatus()).isEqualTo(StageStatus.PENDING);
            assertThat(review.getAssigneeUserId()).isNull();
        });
    }

    @Test
    void aResubmissionResetsTheEarlierRoundsDecisionsAndAssignees() {
        application.setStatus(OnboardingStatus.DRAFT);
        application.setCaseOwnerUserId(null);
        OnboardingStageReview old = OnboardingStageReview.builder().application(application).stage(OnboardingStage.COMPLIANCE)
                .status(StageStatus.APPROVED).assigneeUserId(30L).comments("ok").build();
        when(stageRepository.findByApplicationIdAndStage(APPLICATION, OnboardingStage.COMPLIANCE)).thenReturn(Optional.of(old));
        when(stageRepository.findByApplicationIdAndStage(APPLICATION, OnboardingStage.COMMERCIAL)).thenReturn(Optional.empty());
        when(stageRepository.findByApplicationIdAndStage(APPLICATION, OnboardingStage.TECHNICAL)).thenReturn(Optional.empty());

        service.submitApplication(APPLICATION, APPLICANT);

        assertThat(old.getStatus()).isEqualTo(StageStatus.PENDING);
        assertThat(old.getAssigneeUserId()).isNull();
        assertThat(old.getComments()).isNull();
    }

    @Test
    void submissionIsRefusedWhileTheRequiredDocumentsAreNotReady() {
        application.setStatus(OnboardingStatus.DRAFT);
        application.setCaseOwnerUserId(null);
        org.mockito.Mockito.doThrow(new ConflictException("Required document(s) missing or still being checked: AIR_OPERATOR_CERTIFICATE"))
                .when(documentGate).requireReadyForSubmission(APPLICATION);

        assertThatThrownBy(() -> service.submitApplication(APPLICATION, APPLICANT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("AIR_OPERATOR_CERTIFICATE");

        assertThat(application.getStatus()).isEqualTo(OnboardingStatus.DRAFT);
        verify(stageRepository, never()).save(any(OnboardingStageReview.class));
    }

    // ---------------------------------------------------------------- return

    @Test
    void onlyTheCaseOwnerCanReturnAndTheReturnReleasesTheCase() {
        assertThatThrownBy(() -> service.returnApplication(APPLICATION, "fix", 99L)).isInstanceOf(OperationNotPermittedException.class);

        OnboardingApplicationResponse response = service.returnApplication(APPLICATION, "fix the website", OWNER);

        assertThat(response.getStatus()).isEqualTo("DRAFT");
        assertThat(response.getCaseOwnerUserId()).isNull();
        assertThat(response.getRejectionReason()).isEqualTo("fix the website");
        verify(caseOwnerHistoryRepository).save(any());
    }

    @Test
    void anUnclaimedCaseCannotBeReturned() {
        application.setStatus(OnboardingStatus.SUBMITTED);
        application.setCaseOwnerUserId(null);

        assertThatThrownBy(() -> service.returnApplication(APPLICATION, "fix", OWNER)).isInstanceOf(ConflictException.class);
    }

    // ---------------------------------------------------------------- final decisions

    @Test
    void approvalOnlyWorksOnAReferredCase() {
        assertThatThrownBy(() -> service.approveApplication(APPLICATION, null, APPROVER)).isInstanceOf(ConflictException.class);
        application.setStatus(OnboardingStatus.SUBMITTED);
        assertThatThrownBy(() -> service.approveApplication(APPLICATION, null, APPROVER)).isInstanceOf(ConflictException.class);

        application.setStatus(OnboardingStatus.PENDING_APPROVAL);
        OnboardingApplicationResponse response = service.approveApplication(APPLICATION, "good", APPROVER);

        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(application.getApprovedByUserId()).isEqualTo(APPROVER);
    }

    @Test
    void theApplicantAndTheNomineeCannotApprove() {
        application.setStatus(OnboardingStatus.PENDING_APPROVAL);

        assertThatThrownBy(() -> service.approveApplication(APPLICATION, null, APPLICANT)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.approveApplication(APPLICATION, null, NOMINEE)).isInstanceOf(ConflictException.class);
    }

    @Test
    void aFinalRejectionIsPossibleFromAnyReviewStateButNotAfterApproval() {
        assertThat(service.rejectApplication(APPLICATION, "not viable", APPROVER).getStatus()).isEqualTo("REJECTED");

        application.setStatus(OnboardingStatus.APPROVED);
        assertThatThrownBy(() -> service.rejectApplication(APPLICATION, "late", APPROVER)).isInstanceOf(ConflictException.class);
    }

    // ---------------------------------------------------------------- provisioning

    @Test
    void theProvisionerCannotBeTheApproverTheCaseOwnerTheApplicantOrTheNominee() {
        application.setStatus(OnboardingStatus.APPROVED);
        application.setApprovedByUserId(APPROVER);

        assertThatThrownBy(() -> service.provisionApplication(APPLICATION, null, APPROVER)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.provisionApplication(APPLICATION, null, OWNER)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("owned");
        assertThatThrownBy(() -> service.provisionApplication(APPLICATION, null, APPLICANT)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.provisionApplication(APPLICATION, null, NOMINEE)).isInstanceOf(ConflictException.class);
        verify(airlineRepository, never()).save(any());
    }

    @Test
    void anIndependentProvisionerCreatesTheAirlineAndTheOwnerMembership() {
        application.setStatus(OnboardingStatus.APPROVED);
        application.setApprovedByUserId(APPROVER);

        OnboardingApplicationResponse response = service.provisionApplication(APPLICATION, "go", PROVISIONER);

        assertThat(response.getStatus()).isEqualTo("PROVISIONED");
        assertThat(response.getAirlineId()).isEqualTo(7L);
        verify(airlineMembershipRepository).save(any());
    }

    @Test
    void aCaseThatIsNotApprovedYetCannotBeProvisioned() {
        assertThatThrownBy(() -> service.provisionApplication(APPLICATION, null, PROVISIONER)).isInstanceOf(ConflictException.class);
    }

    @SuppressWarnings("unused")
    private static List<String> unused() {
        return List.of();
    }
}
