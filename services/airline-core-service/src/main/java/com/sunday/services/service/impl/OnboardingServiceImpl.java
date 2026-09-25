package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.request.OnboardingApplicationRequest;
import com.sunday.common_lib.payload.request.OwnerAssignmentRequest;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.OnboardingReviewResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.common_lib.enums.ReviewDecision;
import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.mapper.OnboardingMapper;
import com.sunday.services.model.Airline;
import com.sunday.services.model.AirlineMembership;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingReview;
import com.sunday.services.repository.AirlineMembershipRepository;
import com.sunday.services.repository.AirlineOnboardingApplicationRepository;
import com.sunday.services.repository.AirlineRepository;
import com.sunday.services.repository.OnboardingReviewRepository;
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

    private static final Set<OnboardingStatus> REVIEWABLE_STATUSES =
            Set.of(OnboardingStatus.SUBMITTED, OnboardingStatus.UNDER_REVIEW);
    private static final Set<OnboardingStatus> OWNER_ASSIGNABLE_STATUSES =
            Set.of(OnboardingStatus.SUBMITTED, OnboardingStatus.UNDER_REVIEW, OnboardingStatus.APPROVED);

    private final AirlineOnboardingApplicationRepository applicationRepository;
    private final OnboardingReviewRepository reviewRepository;
    private final AirlineRepository airlineRepository;
    private final AirlineMembershipRepository airlineMembershipRepository;
    private final PlatformUserGuard platformUserGuard;

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
        AirlineOnboardingApplication application = getApplicationOrThrow(applicationId);
        requireOwnedByApplicant(application, applicantUserId);

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
        AirlineOnboardingApplication application = getApplicationOrThrow(applicationId);
        requireOwnedByApplicant(application, applicantUserId);
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
        AirlineOnboardingApplication application = getApplicationOrThrow(applicationId);
        requireOwnedByApplicant(application, applicantUserId);

        if (application.getStatus() != OnboardingStatus.DRAFT) {
            throw new UserException(String.format(
                    ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_SUBMITTABLE, applicationId, application.getStatus()));
        }

        platformUserGuard.requireNotPlatformUser(applicantUserId, ErrorMessageUtil.ONBOARDING_PLATFORM_USER_CANNOT_APPLY);
        requireCompleteForSubmission(application);

        application.setStatus(OnboardingStatus.SUBMITTED);
        application.setRejectionReason(null);
        application.setSubmittedAt(Instant.now());
        return OnboardingMapper.toResponse(applicationRepository.save(application));
    }

    // ---------- Staff side ----------

    @Override
    @Transactional(readOnly = true)
    public Page<OnboardingApplicationResponse> getApplicationsForReview(OnboardingStatus status, Pageable pageable) {
        OnboardingStatus queue = status == null ? OnboardingStatus.SUBMITTED : status;
        if (queue == OnboardingStatus.DRAFT) {
            throw new BadRequestException(ErrorMessageUtil.ONBOARDING_DRAFTS_NOT_LISTABLE);
        }
        return applicationRepository.findByStatus(queue, pageable).map(OnboardingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingApplicationResponse getApplicationForReview(Long applicationId) {
        return OnboardingMapper.toResponse(getVisibleApplicationOrThrow(applicationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingReviewResponse> getReviewHistory(Long applicationId) {
        getVisibleApplicationOrThrow(applicationId);
        return OnboardingMapper.toReviewResponseList(
                reviewRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId));
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse returnApplication(Long applicationId, String comments, Long actorUserId) {
        AirlineOnboardingApplication application = getReviewableOrThrow(applicationId);
        application.setStatus(OnboardingStatus.DRAFT);
        application.setRejectionReason(comments);
        return finishReview(application, actorUserId, ReviewDecision.REQUESTED_CHANGES, comments, null);
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse approveApplication(Long applicationId, String comments, Long actorUserId) {
        AirlineOnboardingApplication application = getReviewableOrThrow(applicationId);
        requireNotApplicantOrNominee(application, actorUserId);
        application.setStatus(OnboardingStatus.APPROVED);
        application.setApprovedByUserId(actorUserId);
        application.setRejectionReason(null);
        return finishReview(application, actorUserId, ReviewDecision.APPROVED, comments, null);
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse rejectApplication(Long applicationId, String comments, Long actorUserId) {
        AirlineOnboardingApplication application = getReviewableOrThrow(applicationId);
        application.setStatus(OnboardingStatus.REJECTED);
        application.setRejectionReason(comments);
        return finishReview(application, actorUserId, ReviewDecision.REJECTED, comments, null);
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse assignOwner(Long applicationId, OwnerAssignmentRequest request, Long actorUserId) {
        AirlineOnboardingApplication application = getVisibleApplicationOrThrow(applicationId);
        if (!OWNER_ASSIGNABLE_STATUSES.contains(application.getStatus())) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.ONBOARDING_OWNER_NOT_ASSIGNABLE, applicationId, application.getStatus()));
        }

        Long ownerUserId = request.getOwnerUserId();
        if (ownerUserId.equals(actorUserId)) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_OWNER_IS_REVIEWER, applicationId));
        }
        platformUserGuard.requireNotPlatformUser(
                ownerUserId, String.format(ErrorMessageUtil.ONBOARDING_PLATFORM_USER_CANNOT_OWN, ownerUserId));

        application.setInitialAdminUserId(ownerUserId);
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        recordReview(saved, actorUserId, ReviewDecision.OWNER_ASSIGNED, request.getComments(), ownerUserId);
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
        AirlineOnboardingApplication application = getVisibleApplicationOrThrow(applicationId);
        if (application.getStatus() != OnboardingStatus.APPROVED) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_APPROVED, applicationId, application.getStatus()));
        }

        Long ownerUserId = application.getInitialAdminUserId();
        if (ownerUserId == null) {
            throw new BadRequestException(ErrorMessageUtil.ONBOARDING_INITIAL_ADMIN_REQUIRED_FOR_APPROVAL);
        }
        if (actorUserId.equals(application.getApprovedByUserId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_SEGREGATION_OF_DUTIES, "approved"));
        }
        requireNotApplicantOrNominee(application, actorUserId);
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
                                                       ReviewDecision decision, String comments, Long ownerUserId) {
        application.setReviewedAt(Instant.now());
        AirlineOnboardingApplication saved = applicationRepository.save(application);
        recordReview(saved, actorUserId, decision, comments, ownerUserId);
        return OnboardingMapper.toResponse(saved);
    }

    private void recordReview(AirlineOnboardingApplication application, Long actorUserId,
                              ReviewDecision decision, String comments, Long ownerUserId) {
        reviewRepository.save(OnboardingReview.builder()
                .application(application)
                .reviewerUserId(actorUserId)
                .decision(decision)
                .comments(comments)
                .assignedOwnerUserId(ownerUserId)
                .build());
    }

    private void requireNotApplicantOrNominee(AirlineOnboardingApplication application, Long actorUserId) {
        if (actorUserId.equals(application.getApplicantUserId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_SEGREGATION_OF_DUTIES, "submitted"));
        }
        if (actorUserId.equals(application.getInitialAdminUserId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_OWNER_IS_REVIEWER, application.getId()));
        }
    }

    /** Staff can never see a draft, even by id: it is the applicant's private working copy. */
    private AirlineOnboardingApplication getVisibleApplicationOrThrow(Long applicationId) {
        AirlineOnboardingApplication application = getApplicationOrThrow(applicationId);
        if (application.getStatus() == OnboardingStatus.DRAFT) {
            throw new ResourceNotFoundException(
                    String.format(ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_FOUND_BY_ID, applicationId));
        }
        return application;
    }

    private AirlineOnboardingApplication getReviewableOrThrow(Long applicationId) {
        AirlineOnboardingApplication application = getVisibleApplicationOrThrow(applicationId);
        if (!REVIEWABLE_STATUSES.contains(application.getStatus())) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_REVIEWABLE, applicationId, application.getStatus()));
        }
        return application;
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

    private void requireOwnedByApplicant(AirlineOnboardingApplication application, Long applicantUserId) {
        if (!application.getApplicantUserId().equals(applicantUserId)) {
            throw new OperationNotPermittedException(String.format(
                    ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_OWNED_BY_APPLICANT, application.getId()));
        }
    }

    private AirlineOnboardingApplication getApplicationOrThrow(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_FOUND_BY_ID, applicationId)));
    }
}
