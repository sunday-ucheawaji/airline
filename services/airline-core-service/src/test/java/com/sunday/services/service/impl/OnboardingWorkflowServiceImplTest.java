package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.CaseOwnerChangeRequest;
import com.sunday.common_lib.payload.request.InformationResponseRequest;
import com.sunday.common_lib.payload.request.StageAssigneeRequest;
import com.sunday.common_lib.payload.response.InformationRequestResponse;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.OnboardingStageResponse;
import com.sunday.services.enums.CaseOwnerAction;
import com.sunday.services.enums.InformationRequestStatus;
import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.enums.StageStatus;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingCaseOwnerHistory;
import com.sunday.services.model.OnboardingInformationRequest;
import com.sunday.services.model.OnboardingStageReview;
import com.sunday.services.repository.AirlineOnboardingApplicationRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnboardingWorkflowServiceImplTest {

    private static final long APPLICATION = 10L;
    private static final long APPLICANT = 1L;
    private static final long NOMINEE = 2L;
    private static final long OWNER = 20L;
    private static final long OTHER_OFFICER = 21L;
    private static final long SPECIALIST = 30L;
    private static final long OTHER_SPECIALIST = 31L;
    private static final long APPROVER = 40L;

    @Mock AirlineOnboardingApplicationRepository applicationRepository;
    @Mock OnboardingReviewRepository reviewRepository;
    @Mock OnboardingCaseOwnerHistoryRepository caseOwnerHistoryRepository;
    @Mock OnboardingInformationRequestRepository informationRequestRepository;
    @Mock OnboardingStageReviewRepository stageRepository;
    @Mock PlatformUserGuard platformUserGuard;
    @Mock ReviewerDirectory reviewerDirectory;
    @Mock OnboardingDocumentGate documentGate;

    private OnboardingWorkflowServiceImpl service;
    private AirlineOnboardingApplication application;

    @BeforeEach
    void setUp() {
        OnboardingSupport support = new OnboardingSupport(
                applicationRepository, reviewRepository, caseOwnerHistoryRepository, informationRequestRepository);
        service = new OnboardingWorkflowServiceImpl(support, applicationRepository, stageRepository,
                informationRequestRepository, caseOwnerHistoryRepository, platformUserGuard, reviewerDirectory, documentGate);

        application = application(OnboardingStatus.UNDER_REVIEW, OWNER);
        when(applicationRepository.findById(APPLICATION)).thenReturn(Optional.of(application));
        when(applicationRepository.save(any(AirlineOnboardingApplication.class))).thenAnswer(i -> i.getArgument(0));
        when(stageRepository.save(any(OnboardingStageReview.class))).thenAnswer(i -> i.getArgument(0));
        when(informationRequestRepository.save(any(OnboardingInformationRequest.class))).thenAnswer(i -> {
            OnboardingInformationRequest r = i.getArgument(0);
            if (r.getId() == null) {
                r.setId(99L);
            }
            return r;
        });
        when(reviewRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ---------------------------------------------------------------- claiming

    @Test
    void anOfficerClaimsASubmittedCaseAndBecomesItsOwner() {
        application.setStatus(OnboardingStatus.SUBMITTED);
        application.setCaseOwnerUserId(null);

        OnboardingApplicationResponse response = service.claim(APPLICATION, OWNER);

        assertThat(response.getStatus()).isEqualTo("UNDER_REVIEW");
        assertThat(response.getCaseOwnerUserId()).isEqualTo(OWNER);
        assertThat(lastOwnerChange().getAction()).isEqualTo(CaseOwnerAction.CLAIMED);
        assertThat(lastOwnerChange().getToUserId()).isEqualTo(OWNER);
    }

    @Test
    void aCaseAlreadyUnderReviewCannotBeClaimed() {
        assertThatThrownBy(() -> service.claim(APPLICATION, OTHER_OFFICER)).isInstanceOf(ConflictException.class);
    }

    @Test
    void theApplicantAndTheNomineeCannotClaimTheirOwnCase() {
        application.setStatus(OnboardingStatus.SUBMITTED);
        application.setCaseOwnerUserId(null);

        assertThatThrownBy(() -> service.claim(APPLICATION, APPLICANT)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.claim(APPLICATION, NOMINEE)).isInstanceOf(ConflictException.class);
    }

    @Test
    void staffCannotClaimADraft() {
        application.setStatus(OnboardingStatus.DRAFT);

        assertThatThrownBy(() -> service.claim(APPLICATION, OWNER)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------- transfer, release, takeover

    @Test
    void theOwnerTransfersTheCaseToAnotherOfficerAndItIsAudited() {
        service.transfer(APPLICATION, OWNER, new CaseOwnerChangeRequest(OTHER_OFFICER, "going on leave"));

        assertThat(application.getCaseOwnerUserId()).isEqualTo(OTHER_OFFICER);
        OnboardingCaseOwnerHistory entry = lastOwnerChange();
        assertThat(entry.getAction()).isEqualTo(CaseOwnerAction.TRANSFERRED);
        assertThat(entry.getFromUserId()).isEqualTo(OWNER);
        assertThat(entry.getToUserId()).isEqualTo(OTHER_OFFICER);
        assertThat(entry.getReason()).isEqualTo("going on leave");
    }

    @Test
    void onlyTheOwnerCanTransferAndTheTargetMustBeAnOfficer() {
        assertThatThrownBy(() -> service.transfer(APPLICATION, OTHER_OFFICER, new CaseOwnerChangeRequest(OWNER, null)))
                .isInstanceOf(OperationNotPermittedException.class);

        doThrow(new BadRequestException("not an officer")).when(platformUserGuard).requireHoldsRole(anyLong(), anyString(), anyString());
        assertThatThrownBy(() -> service.transfer(APPLICATION, OWNER, new CaseOwnerChangeRequest(OTHER_OFFICER, null)))
                .isInstanceOf(BadRequestException.class);
        assertThat(application.getCaseOwnerUserId()).isEqualTo(OWNER);
    }

    @Test
    void aReleasedCaseGoesBackToTheQueueUnowned() {
        service.release(APPLICATION, OWNER, "reassigning workload");

        assertThat(application.getStatus()).isEqualTo(OnboardingStatus.SUBMITTED);
        assertThat(application.getCaseOwnerUserId()).isNull();
        assertThat(lastOwnerChange().getAction()).isEqualTo(CaseOwnerAction.RELEASED);
    }

    @Test
    void onlyASuperAdminCanForceATakeoverAndMustGiveAReason() {
        CaseOwnerChangeRequest withReason = new CaseOwnerChangeRequest(OTHER_OFFICER, "owner unreachable");

        assertThatThrownBy(() -> service.takeOver(APPLICATION, 99L, List.of("ONBOARDING_OFFICER"), withReason))
                .isInstanceOf(OperationNotPermittedException.class);
        assertThatThrownBy(() -> service.takeOver(APPLICATION, 99L, List.of("SUPER_ADMIN"), new CaseOwnerChangeRequest(OTHER_OFFICER, " ")))
                .isInstanceOf(BadRequestException.class);
        verify(applicationRepository, never()).save(any(AirlineOnboardingApplication.class));

        service.takeOver(APPLICATION, 99L, List.of("SUPER_ADMIN"), withReason);

        assertThat(application.getCaseOwnerUserId()).isEqualTo(OTHER_OFFICER);
        OnboardingCaseOwnerHistory entry = lastOwnerChange();
        assertThat(entry.getAction()).isEqualTo(CaseOwnerAction.TAKEN_OVER);
        assertThat(entry.getActorUserId()).isEqualTo(99L);
        assertThat(entry.getReason()).isEqualTo("owner unreachable");
    }

    // ---------------------------------------------------------------- stages

    @Test
    void onlyTheOwnerAssignsAStageAndTheAssigneeMustHoldTheStageRole() {
        assertThatThrownBy(() -> service.assignStage(APPLICATION, OnboardingStage.COMPLIANCE, OTHER_OFFICER, new StageAssigneeRequest(SPECIALIST, null)))
                .isInstanceOf(OperationNotPermittedException.class);

        doThrow(new BadRequestException("lacks role")).when(platformUserGuard).requireHoldsRole(anyLong(), anyString(), anyString());
        assertThatThrownBy(() -> service.assignStage(APPLICATION, OnboardingStage.COMPLIANCE, OWNER, new StageAssigneeRequest(SPECIALIST, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void assigningAStageChecksTheStageSpecificRoleAndResetsAnEarlierDecision() {
        OnboardingStageReview review = stage(OnboardingStage.COMPLIANCE, StageStatus.APPROVED, OTHER_SPECIALIST);
        when(stageRepository.findByApplicationIdAndStage(APPLICATION, OnboardingStage.COMPLIANCE)).thenReturn(Optional.of(review));

        OnboardingStageResponse response = service.assignStage(APPLICATION, OnboardingStage.COMPLIANCE, OWNER, new StageAssigneeRequest(SPECIALIST, "please review"));

        verify(platformUserGuard).requireHoldsRole(org.mockito.ArgumentMatchers.eq(SPECIALIST), org.mockito.ArgumentMatchers.eq("COMPLIANCE_OFFICER"), anyString());
        assertThat(response.getAssigneeUserId()).isEqualTo(SPECIALIST);
        assertThat(response.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void theApplicantOrNomineeCannotBeAssignedAStage() {
        assertThatThrownBy(() -> service.assignStage(APPLICATION, OnboardingStage.TECHNICAL, OWNER, new StageAssigneeRequest(NOMINEE, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void onlyTheAssignedSpecialistCanDecideTheirStage() {
        OnboardingStageReview review = stage(OnboardingStage.COMMERCIAL, StageStatus.PENDING, SPECIALIST);
        when(stageRepository.findByApplicationIdAndStage(APPLICATION, OnboardingStage.COMMERCIAL)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.decideStage(APPLICATION, OnboardingStage.COMMERCIAL, true, OTHER_SPECIALIST, null))
                .isInstanceOf(OperationNotPermittedException.class);

        OnboardingStageResponse response = service.decideStage(APPLICATION, OnboardingStage.COMMERCIAL, true, SPECIALIST, "terms fine");
        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getComments()).isEqualTo("terms fine");
        assertThat(response.getDecidedAt()).isNotNull();
    }

    @Test
    void approvingComplianceRequiresTheRequiredDocumentsToBeVerified() {
        OnboardingStageReview review = stage(OnboardingStage.COMPLIANCE, StageStatus.PENDING, SPECIALIST);
        when(stageRepository.findByApplicationIdAndStage(APPLICATION, OnboardingStage.COMPLIANCE)).thenReturn(Optional.of(review));
        org.mockito.Mockito.doThrow(new ConflictException("Required document(s) not verified yet: OPERATING_LICENSE"))
                .when(documentGate).requireVerifiedForCompliance(APPLICATION);

        assertThatThrownBy(() -> service.decideStage(APPLICATION, OnboardingStage.COMPLIANCE, true, SPECIALIST, "ok"))
                .isInstanceOf(ConflictException.class).hasMessageContaining("not verified");
        assertThat(review.getStatus()).isEqualTo(StageStatus.PENDING);
    }

    @Test
    void theDocumentGateOnlyAppliesToApprovingCompliance() {
        OnboardingStageReview compliance = stage(OnboardingStage.COMPLIANCE, StageStatus.PENDING, SPECIALIST);
        OnboardingStageReview commercial = stage(OnboardingStage.COMMERCIAL, StageStatus.PENDING, SPECIALIST);
        when(stageRepository.findByApplicationIdAndStage(APPLICATION, OnboardingStage.COMPLIANCE)).thenReturn(Optional.of(compliance));
        when(stageRepository.findByApplicationIdAndStage(APPLICATION, OnboardingStage.COMMERCIAL)).thenReturn(Optional.of(commercial));
        org.mockito.Mockito.doThrow(new ConflictException("not verified")).when(documentGate).requireVerifiedForCompliance(APPLICATION);

        assertThat(service.decideStage(APPLICATION, OnboardingStage.COMMERCIAL, true, SPECIALIST, "fine").getStatus()).isEqualTo("APPROVED");
        assertThat(service.decideStage(APPLICATION, OnboardingStage.COMPLIANCE, false, SPECIALIST, "documents unclear").getStatus()).isEqualTo("REJECTED");
        verify(documentGate, never()).requireVerifiedForCompliance(any());
    }

    @Test
    void anUnassignedStageCannotBeDecidedByAnyone() {
        assertThatThrownBy(() -> service.decideStage(APPLICATION, OnboardingStage.TECHNICAL, true, SPECIALIST, null))
                .isInstanceOf(OperationNotPermittedException.class);
    }

    @Test
    void aRejectionNeedsAReason() {
        OnboardingStageReview review = stage(OnboardingStage.TECHNICAL, StageStatus.PENDING, SPECIALIST);
        when(stageRepository.findByApplicationIdAndStage(APPLICATION, OnboardingStage.TECHNICAL)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> service.decideStage(APPLICATION, OnboardingStage.TECHNICAL, false, SPECIALIST, "  "))
                .isInstanceOf(BadRequestException.class);
        assertThat(service.decideStage(APPLICATION, OnboardingStage.TECHNICAL, false, SPECIALIST, "no test environment").getStatus())
                .isEqualTo("REJECTED");
    }

    // ---------------------------------------------------------------- referral gate

    @Test
    void aCaseCannotBeReferredWhileAStageIsPendingOrRejected() {
        stages(StageStatus.APPROVED, StageStatus.PENDING, StageStatus.APPROVED);
        assertThatThrownBy(() -> service.refer(APPLICATION, OWNER, null))
                .isInstanceOf(ConflictException.class).hasMessageContaining("COMMERCIAL");

        stages(StageStatus.APPROVED, StageStatus.APPROVED, StageStatus.REJECTED);
        assertThatThrownBy(() -> service.refer(APPLICATION, OWNER, null))
                .isInstanceOf(ConflictException.class).hasMessageContaining("TECHNICAL");
        assertThat(application.getStatus()).isEqualTo(OnboardingStatus.UNDER_REVIEW);
    }

    @Test
    void aCaseCannotBeReferredWhileAnInformationRequestIsOpen() {
        stages(StageStatus.APPROVED, StageStatus.APPROVED, StageStatus.APPROVED);
        when(informationRequestRepository.existsByApplicationIdAndStatus(APPLICATION, InformationRequestStatus.OPEN)).thenReturn(true);

        assertThatThrownBy(() -> service.refer(APPLICATION, OWNER, null)).isInstanceOf(ConflictException.class);
    }

    @Test
    void theOwnerReferringAFullyApprovedCaseMovesItToPendingApproval() {
        stages(StageStatus.APPROVED, StageStatus.APPROVED, StageStatus.APPROVED);

        assertThat(service.refer(APPLICATION, OWNER, "all clear").getStatus()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void onlyTheOwnerMayReferAndOnlyAClaimedCase() {
        stages(StageStatus.APPROVED, StageStatus.APPROVED, StageStatus.APPROVED);

        assertThatThrownBy(() -> service.refer(APPLICATION, OTHER_OFFICER, null)).isInstanceOf(OperationNotPermittedException.class);
    }

    @Test
    void theApproverCanSendAReferredCaseBackButMustSayWhy() {
        application.setStatus(OnboardingStatus.PENDING_APPROVAL);

        assertThatThrownBy(() -> service.sendBack(APPLICATION, APPROVER, " ")).isInstanceOf(BadRequestException.class);
        assertThat(service.sendBack(APPLICATION, APPROVER, "check the licence again").getStatus()).isEqualTo("UNDER_REVIEW");
    }

    @Test
    void onlyAReferredCaseCanBeSentBack() {
        assertThatThrownBy(() -> service.sendBack(APPLICATION, APPROVER, "why not")).isInstanceOf(ConflictException.class);
    }

    // ---------------------------------------------------------------- information requests

    @Test
    void theOwnerAsksOnBehalfOfTheWholeReviewAndASpecialistOnlyForTheirOwnStage() {
        InformationRequestResponse ownerRequest = service.requestInformation(APPLICATION, null, OWNER, "please confirm the address");
        assertThat(ownerRequest.getStage()).isNull();
        assertThat(ownerRequest.getStatus()).isEqualTo("OPEN");

        assertThatThrownBy(() -> service.requestInformation(APPLICATION, null, OTHER_OFFICER, "x")).isInstanceOf(OperationNotPermittedException.class);

        OnboardingStageReview review = stage(OnboardingStage.COMPLIANCE, StageStatus.PENDING, SPECIALIST);
        when(stageRepository.findByApplicationIdAndStage(APPLICATION, OnboardingStage.COMPLIANCE)).thenReturn(Optional.of(review));
        assertThat(service.requestInformation(APPLICATION, OnboardingStage.COMPLIANCE, SPECIALIST, "proof of registration").getStage())
                .isEqualTo("COMPLIANCE");
        assertThatThrownBy(() -> service.requestInformation(APPLICATION, OnboardingStage.COMPLIANCE, OTHER_SPECIALIST, "x"))
                .isInstanceOf(OperationNotPermittedException.class);
    }

    @Test
    void theApplicantAnswersAnOpenRequestOnceAndOnlyTheirOwn() {
        OnboardingInformationRequest request = openRequest();
        when(informationRequestRepository.findByIdAndApplicationId(99L, APPLICATION)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.respond(APPLICATION, 99L, 555L, new InformationResponseRequest("x")))
                .isInstanceOf(OperationNotPermittedException.class);

        InformationRequestResponse answered = service.respond(APPLICATION, 99L, APPLICANT, new InformationResponseRequest("here it is"));
        assertThat(answered.getStatus()).isEqualTo("ANSWERED");
        assertThat(answered.getResponse()).isEqualTo("here it is");

        assertThatThrownBy(() -> service.respond(APPLICATION, 99L, APPLICANT, new InformationResponseRequest("again")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void respondingToAnUnknownRequestIsNotFound() {
        when(informationRequestRepository.findByIdAndApplicationId(7L, APPLICATION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.respond(APPLICATION, 7L, APPLICANT, new InformationResponseRequest("x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------- withdrawal

    @Test
    void theApplicantWithdrawsAnApplicationInReviewAndOpenRequestsAreCancelled() {
        OnboardingInformationRequest open = openRequest();
        when(informationRequestRepository.findByApplicationIdAndStatus(APPLICATION, InformationRequestStatus.OPEN)).thenReturn(List.of(open));

        assertThat(service.withdraw(APPLICATION, APPLICANT).getStatus()).isEqualTo("WITHDRAWN");
        assertThat(open.getStatus()).isEqualTo(InformationRequestStatus.CANCELLED);
    }

    @Test
    void aReferredOrApprovedApplicationCannotBeWithdrawnAndNeitherCanSomeoneElsesApplication() {
        assertThatThrownBy(() -> service.withdraw(APPLICATION, 555L)).isInstanceOf(OperationNotPermittedException.class);

        application.setStatus(OnboardingStatus.PENDING_APPROVAL);
        assertThatThrownBy(() -> service.withdraw(APPLICATION, APPLICANT)).isInstanceOf(ConflictException.class);

        application.setStatus(OnboardingStatus.APPROVED);
        assertThatThrownBy(() -> service.withdraw(APPLICATION, APPLICANT)).isInstanceOf(ConflictException.class);
    }

    // ---------------------------------------------------------------- helpers

    private static long anyLong() {
        return org.mockito.ArgumentMatchers.anyLong();
    }

    private OnboardingCaseOwnerHistory lastOwnerChange() {
        ArgumentCaptor<OnboardingCaseOwnerHistory> captor = ArgumentCaptor.forClass(OnboardingCaseOwnerHistory.class);
        verify(caseOwnerHistoryRepository).save(captor.capture());
        return captor.getValue();
    }

    private void stages(StageStatus compliance, StageStatus commercial, StageStatus technical) {
        when(stageRepository.findByApplicationId(APPLICATION)).thenReturn(List.of(
                stage(OnboardingStage.COMPLIANCE, compliance, null),
                stage(OnboardingStage.COMMERCIAL, commercial, null),
                stage(OnboardingStage.TECHNICAL, technical, null)));
    }

    private OnboardingStageReview stage(OnboardingStage stage, StageStatus status, Long assignee) {
        return OnboardingStageReview.builder().application(application).stage(stage).status(status).assigneeUserId(assignee).build();
    }

    private OnboardingInformationRequest openRequest() {
        return OnboardingInformationRequest.builder().id(99L).application(application).stage(OnboardingStage.COMPLIANCE)
                .requestedByUserId(SPECIALIST).message("q").build();
    }

    private static AirlineOnboardingApplication application(OnboardingStatus status, Long caseOwner) {
        AirlineOnboardingApplication a = new AirlineOnboardingApplication();
        a.setId(APPLICATION);
        a.setApplicantUserId(APPLICANT);
        a.setInitialAdminUserId(NOMINEE);
        a.setStatus(status);
        a.setCaseOwnerUserId(caseOwner);
        return a;
    }
}
