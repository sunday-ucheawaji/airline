package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.PlatformRoles;
import com.sunday.common_lib.enums.ReviewDecision;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.CaseOwnerChangeRequest;
import com.sunday.common_lib.payload.request.InformationResponseRequest;
import com.sunday.common_lib.payload.request.StageAssigneeRequest;
import com.sunday.common_lib.payload.response.CaseOwnerHistoryResponse;
import com.sunday.common_lib.payload.response.InformationRequestResponse;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.OnboardingReviewResponse;
import com.sunday.common_lib.payload.response.OnboardingStageResponse;
import com.sunday.common_lib.payload.response.ReviewerResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.enums.CaseOwnerAction;
import com.sunday.services.enums.InformationRequestStatus;
import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.enums.StageStatus;
import com.sunday.services.mapper.OnboardingMapper;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingInformationRequest;
import com.sunday.services.model.OnboardingStageReview;
import com.sunday.services.repository.AirlineOnboardingApplicationRepository;
import com.sunday.services.repository.OnboardingCaseOwnerHistoryRepository;
import com.sunday.services.repository.OnboardingInformationRequestRepository;
import com.sunday.services.repository.OnboardingStageReviewRepository;
import com.sunday.services.service.OnboardingWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OnboardingWorkflowServiceImpl implements OnboardingWorkflowService {

    private static final Set<OnboardingStatus> UNDER_REVIEW = Set.of(OnboardingStatus.UNDER_REVIEW);
    private static final Set<OnboardingStatus> OWNER_CHANGEABLE =
            Set.of(OnboardingStatus.UNDER_REVIEW, OnboardingStatus.PENDING_APPROVAL);
    private static final Set<OnboardingStatus> WITHDRAWABLE =
            Set.of(OnboardingStatus.DRAFT, OnboardingStatus.SUBMITTED, OnboardingStatus.UNDER_REVIEW);

    private final OnboardingSupport support;
    private final AirlineOnboardingApplicationRepository applicationRepository;
    private final OnboardingStageReviewRepository stageRepository;
    private final OnboardingInformationRequestRepository informationRequestRepository;
    private final OnboardingCaseOwnerHistoryRepository caseOwnerHistoryRepository;
    private final PlatformUserGuard platformUserGuard;
    private final ReviewerDirectory reviewerDirectory;
    private final OnboardingDocumentGate documentGate;

    // ---------- Case ownership ----------

    @Override
    @Transactional
    public OnboardingApplicationResponse claim(Long applicationId, Long actorUserId) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, Set.of(OnboardingStatus.SUBMITTED), ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_CLAIMABLE);
        if (application.getCaseOwnerUserId() != null) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_CASE_ALREADY_CLAIMED, applicationId));
        }
        requireNotParty(application, actorUserId);

        application.setCaseOwnerUserId(actorUserId);
        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        support.recordOwnerChange(saved, null, actorUserId, actorUserId, CaseOwnerAction.CLAIMED, null);
        return OnboardingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse transfer(Long applicationId, Long actorUserId, CaseOwnerChangeRequest request) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, UNDER_REVIEW, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_UNDER_REVIEW);
        support.requireCaseOwner(application, actorUserId);
        return changeCaseOwner(application, actorUserId, request.getNewCaseOwnerUserId(), CaseOwnerAction.TRANSFERRED, request.getReason());
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse release(Long applicationId, Long actorUserId, String reason) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, UNDER_REVIEW, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_UNDER_REVIEW);
        support.requireCaseOwner(application, actorUserId);

        application.setCaseOwnerUserId(null);
        application.setStatus(OnboardingStatus.SUBMITTED);
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        support.recordOwnerChange(saved, actorUserId, null, actorUserId, CaseOwnerAction.RELEASED, reason);
        return OnboardingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse takeOver(Long applicationId, Long actorUserId, Collection<String> actorRoles,
                                                  CaseOwnerChangeRequest request) {
        if (!actorRoles.contains(PlatformRoles.SUPER_ADMIN)) {
            throw new OperationNotPermittedException(ErrorMessageUtil.ONBOARDING_TAKEOVER_SUPER_ADMIN_ONLY);
        }
        if (!StringUtils.hasText(request.getReason())) {
            throw new BadRequestException(ErrorMessageUtil.ONBOARDING_REASON_MANDATORY);
        }
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, OWNER_CHANGEABLE, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_UNDER_REVIEW);
        if (application.getCaseOwnerUserId() == null) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_CASE_NOT_CLAIMED, applicationId));
        }
        return changeCaseOwner(application, actorUserId, request.getNewCaseOwnerUserId(), CaseOwnerAction.TAKEN_OVER, request.getReason());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseOwnerHistoryResponse> getCaseOwnerHistory(Long applicationId) {
        support.getVisibleOrThrow(applicationId);
        return caseOwnerHistoryRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(OnboardingMapper::toCaseOwnerHistoryResponse)
                .toList();
    }

    @Override
    public List<ReviewerResponse> getReviewers(OnboardingStage stage) {
        return reviewerDirectory.byRole(stage == null ? PlatformRoles.ONBOARDING_OFFICER : stage.requiredRole());
    }

    // ---------- Stages ----------

    @Override
    @Transactional
    public OnboardingStageResponse assignStage(Long applicationId, OnboardingStage stage, Long actorUserId, StageAssigneeRequest request) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, UNDER_REVIEW, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_UNDER_REVIEW);
        support.requireCaseOwner(application, actorUserId);

        Long assigneeUserId = request.getAssigneeUserId();
        if (support.isParty(application, assigneeUserId)) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_REVIEWER_IS_PARTY, applicationId));
        }
        platformUserGuard.requireHoldsRole(assigneeUserId, stage.requiredRole(),
                String.format(ErrorMessageUtil.ONBOARDING_STAGE_ASSIGNEE_LACKS_ROLE, assigneeUserId, stage.requiredRole(), stage));

        OnboardingStageReview review = stageFor(application, stage);
        review.setAssigneeUserId(assigneeUserId);
        resetDecision(review);
        OnboardingStageReview saved = stageRepository.save(review);
        support.record(application, actorUserId, ReviewDecision.STAGE_ASSIGNED, request.getComments(), assigneeUserId, stage);
        return OnboardingMapper.toStageResponse(saved);
    }

    @Override
    @Transactional
    public OnboardingStageResponse decideStage(Long applicationId, OnboardingStage stage, boolean approve, Long actorUserId, String comments) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, UNDER_REVIEW, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_UNDER_REVIEW);

        OnboardingStageReview review = stageFor(application, stage);
        requireAssignedTo(review, application, stage, actorUserId);
        support.requireNotApplicantOrNominee(application, actorUserId);
        if (!approve && !StringUtils.hasText(comments)) {
            throw new BadRequestException(ErrorMessageUtil.ONBOARDING_REASON_MANDATORY);
        }
        if (approve && stage == OnboardingStage.COMPLIANCE) {
            documentGate.requireVerifiedForCompliance(applicationId);
        }

        review.setStatus(approve ? StageStatus.APPROVED : StageStatus.REJECTED);
        review.setComments(comments);
        review.setDecidedAt(Instant.now());
        OnboardingStageReview saved = stageRepository.save(review);
        support.record(application, actorUserId, approve ? ReviewDecision.STAGE_APPROVED : ReviewDecision.STAGE_REJECTED,
                comments, null, stage);
        return OnboardingMapper.toStageResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingStageResponse> getStages(Long applicationId) {
        support.getVisibleOrThrow(applicationId);
        return stageRepository.findByApplicationId(applicationId).stream()
                .sorted(Comparator.comparing(OnboardingStageReview::getStage))
                .map(OnboardingMapper::toStageResponse)
                .toList();
    }

    // ---------- Information requests, comments ----------

    @Override
    @Transactional
    public InformationRequestResponse requestInformation(Long applicationId, OnboardingStage stage, Long actorUserId, String message) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, UNDER_REVIEW, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_UNDER_REVIEW);

        if (stage == null) {
            support.requireCaseOwner(application, actorUserId);
        } else {
            requireAssignedTo(stageFor(application, stage), application, stage, actorUserId);
        }

        OnboardingInformationRequest saved = informationRequestRepository.save(OnboardingInformationRequest.builder()
                .application(application)
                .stage(stage)
                .requestedByUserId(actorUserId)
                .message(message)
                .build());
        support.record(application, actorUserId, ReviewDecision.INFORMATION_REQUESTED, message, null, stage);
        return OnboardingMapper.toInformationRequestResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InformationRequestResponse> getInformationRequests(Long applicationId) {
        support.getVisibleOrThrow(applicationId);
        return informationRequestRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(OnboardingMapper::toInformationRequestResponse)
                .toList();
    }

    @Override
    @Transactional
    public OnboardingReviewResponse comment(Long applicationId, Long actorUserId, String message) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        return OnboardingMapper.toReviewResponse(
                support.record(application, actorUserId, ReviewDecision.COMMENTED, message, null, null));
    }

    // ---------- Hand-off to final approval ----------

    @Override
    @Transactional
    public OnboardingApplicationResponse refer(Long applicationId, Long actorUserId, String comments) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, UNDER_REVIEW, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_UNDER_REVIEW);
        support.requireCaseOwner(application, actorUserId);

        List<OnboardingStageReview> stages = stageRepository.findByApplicationId(applicationId);
        List<OnboardingStage> notApprovedStages = Arrays.stream(OnboardingStage.values())
                .filter(stage -> stages.stream().noneMatch(
                        s -> s.getStage() == stage && s.getStatus() == StageStatus.APPROVED))
                .toList();
        if (!notApprovedStages.isEmpty()) {
            String stageNames = notApprovedStages.stream().map(OnboardingStage::name).collect(Collectors.joining(", "));
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_STAGES_NOT_APPROVED, applicationId, stageNames));
        }
        if (informationRequestRepository.existsByApplicationIdAndStatus(applicationId, InformationRequestStatus.OPEN)) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_OPEN_INFORMATION_REQUESTS, applicationId));
        }

        application.setStatus(OnboardingStatus.PENDING_APPROVAL);
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        support.record(saved, actorUserId, ReviewDecision.REFERRED, comments, null, null);
        return OnboardingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse sendBack(Long applicationId, Long actorUserId, String comments) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, Set.of(OnboardingStatus.PENDING_APPROVAL),
                ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_PENDING_APPROVAL);
        if (!StringUtils.hasText(comments)) {
            throw new BadRequestException(ErrorMessageUtil.ONBOARDING_COMMENT_MANDATORY_FOR_SEND_BACK);
        }

        application.setStatus(OnboardingStatus.UNDER_REVIEW);
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        support.record(saved, actorUserId, ReviewDecision.SENT_BACK, comments, null, null);
        return OnboardingMapper.toResponse(saved);
    }

    // ---------- Applicant side ----------

    @Override
    @Transactional
    public OnboardingApplicationResponse withdraw(Long applicationId, Long applicantUserId) {
        AirlineOnboardingApplication application = support.getOrThrow(applicationId);
        support.requireOwnedByApplicant(application, applicantUserId);
        support.requireStatus(application, WITHDRAWABLE, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_WITHDRAWABLE);

        support.cancelOpenInformationRequests(application);
        application.setStatus(OnboardingStatus.WITHDRAWN);
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        support.record(saved, applicantUserId, ReviewDecision.WITHDRAWN, null, null, null);
        return OnboardingMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InformationRequestResponse> getMyInformationRequests(Long applicationId, Long applicantUserId) {
        support.requireOwnedByApplicant(support.getOrThrow(applicationId), applicantUserId);
        return informationRequestRepository.findByApplicationIdOrderByCreatedAtAsc(applicationId).stream()
                .map(OnboardingMapper::toInformationRequestResponse)
                .toList();
    }

    @Override
    @Transactional
    public InformationRequestResponse respond(Long applicationId, Long requestId, Long applicantUserId, InformationResponseRequest request) {
        AirlineOnboardingApplication application = support.getOrThrow(applicationId);
        support.requireOwnedByApplicant(application, applicantUserId);

        OnboardingInformationRequest informationRequest = informationRequestRepository.findByIdAndApplicationId(requestId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.ONBOARDING_INFORMATION_REQUEST_NOT_FOUND, requestId)));
        if (informationRequest.getStatus() != InformationRequestStatus.OPEN) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_INFORMATION_REQUEST_NOT_OPEN, requestId));
        }

        informationRequest.setResponse(request.getResponse());
        informationRequest.setStatus(InformationRequestStatus.ANSWERED);
        informationRequest.setRespondedAt(Instant.now());
        OnboardingInformationRequest saved = informationRequestRepository.save(informationRequest);
        support.record(application, applicantUserId, ReviewDecision.INFORMATION_PROVIDED, request.getResponse(), null, saved.getStage());
        return OnboardingMapper.toInformationRequestResponse(saved);
    }

    // ---------- Helpers ----------

    private OnboardingApplicationResponse changeCaseOwner(AirlineOnboardingApplication application, Long actorUserId,
                                                          Long newOwnerUserId, CaseOwnerAction action, String reason) {
        Long currentOwner = application.getCaseOwnerUserId();
        if (newOwnerUserId.equals(currentOwner)) {
            throw new BadRequestException(String.format(ErrorMessageUtil.ONBOARDING_CASE_ALREADY_CLAIMED, application.getId()));
        }
        requireNotParty(application, newOwnerUserId);
        platformUserGuard.requireHoldsRole(newOwnerUserId, PlatformRoles.ONBOARDING_OFFICER,
                String.format(ErrorMessageUtil.ONBOARDING_CASE_OWNER_MUST_BE_OFFICER, newOwnerUserId, PlatformRoles.ONBOARDING_OFFICER));

        application.setCaseOwnerUserId(newOwnerUserId);
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        support.recordOwnerChange(saved, currentOwner, newOwnerUserId, actorUserId, action, reason);
        return OnboardingMapper.toResponse(saved);
    }

    private void requireNotParty(AirlineOnboardingApplication application, Long userId) {
        if (support.isParty(application, userId)) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_CASE_OWNER_IS_PARTY, application.getId()));
        }
    }

    private void requireAssignedTo(OnboardingStageReview review, AirlineOnboardingApplication application,
                                   OnboardingStage stage, Long actorUserId) {
        if (!actorUserId.equals(review.getAssigneeUserId())) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.ONBOARDING_STAGE_NOT_ASSIGNED_TO_USER, stage, application.getId()));
        }
    }

    /** The stage row for an application; created on demand for applications submitted before stages existed. */
    private OnboardingStageReview stageFor(AirlineOnboardingApplication application, OnboardingStage stage) {
        return stageRepository.findByApplicationIdAndStage(application.getId(), stage)
                .orElseGet(() -> OnboardingStageReview.builder().application(application).stage(stage).build());
    }

    private void resetDecision(OnboardingStageReview review) {
        review.setStatus(StageStatus.PENDING);
        review.setComments(null);
        review.setDecidedAt(null);
    }
}
