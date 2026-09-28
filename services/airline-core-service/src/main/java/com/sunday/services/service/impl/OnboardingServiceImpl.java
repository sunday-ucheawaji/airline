package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.request.OnboardingApplicationRequest;
import com.sunday.common_lib.payload.request.OwnerAssignmentRequest;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.OnboardingReviewResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.common_lib.enums.ReviewDecision;
import com.sunday.services.enums.CaseOwnerAction;
import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.enums.StageStatus;
import com.sunday.services.mapper.OnboardingMapper;
import com.sunday.services.model.Airline;
import com.sunday.services.model.AirlineMembership;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingStageReview;
import com.sunday.services.repository.AirlineMembershipRepository;
import com.sunday.services.repository.AirlineOnboardingApplicationRepository;
import com.sunday.services.repository.AirlineRepository;
import com.sunday.services.repository.OnboardingReviewRepository;
import com.sunday.services.repository.OnboardingStageReviewRepository;
import com.sunday.services.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private static final Set<OnboardingStatus> UNDER_REVIEW = Set.of(OnboardingStatus.UNDER_REVIEW);
    private static final Set<OnboardingStatus> PENDING_APPROVAL = Set.of(OnboardingStatus.PENDING_APPROVAL);
    /** Where a final rejection is still possible. */
    private static final Set<OnboardingStatus> REVIEWABLE_STATUSES =
            Set.of(OnboardingStatus.SUBMITTED, OnboardingStatus.UNDER_REVIEW, OnboardingStatus.PENDING_APPROVAL);
    private static final Set<OnboardingStatus> OWNER_ASSIGNABLE_STATUSES = Set.of(
            OnboardingStatus.SUBMITTED, OnboardingStatus.UNDER_REVIEW, OnboardingStatus.PENDING_APPROVAL, OnboardingStatus.APPROVED);

    private final AirlineOnboardingApplicationRepository applicationRepository;
    private final OnboardingReviewRepository reviewRepository;
    private final OnboardingStageReviewRepository stageRepository;
    private final AirlineRepository airlineRepository;
    private final AirlineMembershipRepository airlineMembershipRepository;
    private final PlatformUserGuard platformUserGuard;
    private final OnboardingSupport support;
    private final OnboardingDocumentGate documentGate;

    @Value("${airline.owner-role-id}")
    private Long ownerRoleId;

    // ---------- Applicant side ----------

    @Override
    @Transactional
    public OnboardingApplicationResponse createDraft(OnboardingApplicationRequest request, Long applicantUserId) {
        platformUserGuard.requireNotPlatformUser(applicantUserId, ErrorMessageUtil.ONBOARDING_PLATFORM_USER_CANNOT_APPLY);
        AirlineOnboardingApplication application = new AirlineOnboardingApplication();
        application.setApplicantUserId(applicantUserId);
        application.setStatus(OnboardingStatus.DRAFT);
        OnboardingMapper.applyToEntity(application, request);
        return OnboardingMapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse updateDraft(Long applicationId, OnboardingApplicationRequest request, Long applicantUserId) {
        AirlineOnboardingApplication application = support.getOrThrow(applicationId);
        support.requireOwnedByApplicant(application, applicantUserId);

        if (application.getStatus() != OnboardingStatus.DRAFT) {
            throw new UserException(String.format(
                    ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_EDITABLE, applicationId, application.getStatus()));
        }

        OnboardingMapper.applyToEntity(application, request);
        return OnboardingMapper.toResponse(applicationRepository.save(application));
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingApplicationResponse getMyApplicationById(Long applicationId, Long applicantUserId) {
        AirlineOnboardingApplication application = support.getOrThrow(applicationId);
        support.requireOwnedByApplicant(application, applicantUserId);
        return OnboardingMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingApplicationResponse> getMyApplications(Long applicantUserId) {
        return OnboardingMapper.toResponseList(applicationRepository.findByApplicantUserId(applicantUserId));
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse submitApplication(Long applicationId, Long applicantUserId) {
        AirlineOnboardingApplication application = support.getOrThrow(applicationId);
        support.requireOwnedByApplicant(application, applicantUserId);

        if (application.getStatus() != OnboardingStatus.DRAFT) {
            throw new UserException(String.format(
                    ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_SUBMITTABLE, applicationId, application.getStatus()));
        }

        platformUserGuard.requireNotPlatformUser(applicantUserId, ErrorMessageUtil.ONBOARDING_PLATFORM_USER_CANNOT_APPLY);
        requireCompleteForSubmission(application);
        documentGate.requireReadyForSubmission(applicationId);

        application.setStatus(OnboardingStatus.SUBMITTED);
        application.setRejectionReason(null);
        application.setSubmittedAt(Instant.now());
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        startFreshStageRound(saved);
        return OnboardingMapper.toResponse(saved);
    }

    // ---------- Staff side ----------

    @Override
    @Transactional(readOnly = true)
    public Page<OnboardingApplicationResponse> getApplicationsForReview(OnboardingStatus status, Long caseOwnerUserId, Pageable pageable) {
        OnboardingStatus queue = status == null ? OnboardingStatus.SUBMITTED : status;
        if (queue == OnboardingStatus.DRAFT) {
            throw new BadRequestException(ErrorMessageUtil.ONBOARDING_DRAFTS_NOT_LISTABLE);
        }
        Page<AirlineOnboardingApplication> page = caseOwnerUserId == null
                ? applicationRepository.findByStatus(queue, pageable)
                : applicationRepository.findByStatusAndCaseOwnerUserId(queue, caseOwnerUserId, pageable);
        return page.map(OnboardingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingApplicationResponse getApplicationForReview(Long applicationId) {
        return OnboardingMapper.toResponse(support.getVisibleOrThrow(applicationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingReviewResponse> getReviewHistory(Long applicationId) {
        support.getVisibleOrThrow(applicationId);
        return OnboardingMapper.toReviewResponseList(
                reviewRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId));
    }

    /** Case owner only: sends the application back to the applicant, releasing the case. */
    @Override
    @Transactional
    public OnboardingApplicationResponse returnApplication(Long applicationId, String comments, Long actorUserId) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, UNDER_REVIEW, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_UNDER_REVIEW);
        support.requireCaseOwner(application, actorUserId);

        support.cancelOpenInformationRequests(application);
        support.recordOwnerChange(application, actorUserId, null, actorUserId, CaseOwnerAction.RELEASED, "Returned to the applicant");
        application.setCaseOwnerUserId(null);
        application.setStatus(OnboardingStatus.DRAFT);
        application.setRejectionReason(comments);
        return finishReview(application, actorUserId, ReviewDecision.REQUESTED_CHANGES, comments, null);
    }

    /** Final approval: only for cases the case owner has referred. */
    @Override
    @Transactional
    public OnboardingApplicationResponse approveApplication(Long applicationId, String comments, Long actorUserId) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, PENDING_APPROVAL, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_PENDING_APPROVAL);
        support.requireNotApplicantOrNominee(application, actorUserId);
        application.setStatus(OnboardingStatus.APPROVED);
        application.setApprovedByUserId(actorUserId);
        application.setRejectionReason(null);
        return finishReview(application, actorUserId, ReviewDecision.APPROVED, comments, null);
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse rejectApplication(Long applicationId, String comments, Long actorUserId) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, REVIEWABLE_STATUSES, ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_REVIEWABLE);
        support.cancelOpenInformationRequests(application);
        application.setStatus(OnboardingStatus.REJECTED);
        application.setRejectionReason(comments);
        return finishReview(application, actorUserId, ReviewDecision.REJECTED, comments, null);
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse assignOwner(Long applicationId, OwnerAssignmentRequest request, Long actorUserId) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, OWNER_ASSIGNABLE_STATUSES, ErrorMessageUtil.ONBOARDING_OWNER_NOT_ASSIGNABLE);

        Long ownerUserId = request.getOwnerUserId();
        if (ownerUserId.equals(actorUserId)) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_OWNER_IS_REVIEWER, applicationId));
        }
        platformUserGuard.requireNotPlatformUser(
                ownerUserId, String.format(ErrorMessageUtil.ONBOARDING_PLATFORM_USER_CANNOT_OWN, ownerUserId));

        application.setInitialAdminUserId(ownerUserId);
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        support.record(saved, actorUserId, ReviewDecision.OWNER_ASSIGNED, request.getComments(), ownerUserId, null);
        return OnboardingMapper.toResponse(saved);
    }

    // Provisioning creates an Airline + owner membership, so the per-user airline list and the
    // ACTIVE-airlines dropdown must be evicted or they stay stale until their TTL expires.
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "airlinesByUser", allEntries = true),
            @CacheEvict(cacheNames = "airlinesDropdown", allEntries = true)
    })
    public OnboardingApplicationResponse provisionApplication(Long applicationId, String comments, Long actorUserId) {
        AirlineOnboardingApplication application = support.getVisibleOrThrow(applicationId);
        support.requireStatus(application, Set.of(OnboardingStatus.APPROVED), ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_APPROVED);

        Long ownerUserId = application.getInitialAdminUserId();
        if (ownerUserId == null) {
            throw new BadRequestException(ErrorMessageUtil.ONBOARDING_INITIAL_ADMIN_REQUIRED_FOR_APPROVAL);
        }
        if (actorUserId.equals(application.getApprovedByUserId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_SEGREGATION_OF_DUTIES, "approved"));
        }
        if (actorUserId.equals(application.getCaseOwnerUserId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_SEGREGATION_OF_DUTIES, "owned"));
        }
        support.requireNotApplicantOrNominee(application, actorUserId);
        platformUserGuard.requireNotPlatformUser(
                ownerUserId, String.format(ErrorMessageUtil.ONBOARDING_PLATFORM_USER_CANNOT_OWN, ownerUserId));

        Airline airline = airlineRepository.save(OnboardingMapper.toAirlineEntity(application));
        airlineMembershipRepository.save(AirlineMembership.builder()
                .airline(airline)
                .userId(ownerUserId)
                .roleId(ownerRoleId)
                .status(MembershipStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build());

        application.setStatus(OnboardingStatus.PROVISIONED);
        application.setAirlineId(airline.getId());
        application.setRejectionReason(null);
        return finishReview(application, actorUserId, ReviewDecision.PROVISIONED, comments, ownerUserId);
    }

    // ---------- Helpers ----------

    private OnboardingApplicationResponse finishReview(AirlineOnboardingApplication application, Long actorUserId,
                                                       ReviewDecision decision, String comments, Long targetUserId) {
        application.setReviewedAt(Instant.now());
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        support.record(saved, actorUserId, decision, comments, targetUserId, null);
        return OnboardingMapper.toResponse(saved);
    }

    /** Every submission starts a new review round: one PENDING, unassigned stage per specialist area. */
    private void startFreshStageRound(AirlineOnboardingApplication application) {
        for (OnboardingStage stage : OnboardingStage.values()) {
            OnboardingStageReview review = stageRepository.findByApplicationIdAndStage(application.getId(), stage)
                    .orElseGet(() -> OnboardingStageReview.builder().application(application).stage(stage).build());
            review.setStatus(StageStatus.PENDING);
            review.setAssigneeUserId(null);
            review.setComments(null);
            review.setDecidedAt(null);
            stageRepository.save(review);
        }
    }

    private void requireCompleteForSubmission(AirlineOnboardingApplication application) {
        if (!StringUtils.hasText(application.getLegalName())) {
            throw new UserException(ErrorMessageUtil.ONBOARDING_LEGAL_NAME_MANDATORY);
        }
        if (!StringUtils.hasText(application.getDisplayName())) {
            throw new UserException(ErrorMessageUtil.ONBOARDING_DISPLAY_NAME_MANDATORY);
        }
        if (!StringUtils.hasText(application.getCountry())) {
            throw new UserException(ErrorMessageUtil.ONBOARDING_COUNTRY_MANDATORY);
        }
        if (!StringUtils.hasText(application.getRegistrationNumber())) {
            throw new UserException(ErrorMessageUtil.ONBOARDING_REGISTRATION_NUMBER_MANDATORY);
        }
    }
}
