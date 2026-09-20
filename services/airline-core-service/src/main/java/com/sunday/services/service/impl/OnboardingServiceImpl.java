package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.UserException;
import com.sunday.common_lib.payload.request.OnboardingApplicationRequest;
import com.sunday.common_lib.payload.request.OnboardingReviewRequest;
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

    private final AirlineOnboardingApplicationRepository applicationRepository;
    private final OnboardingReviewRepository reviewRepository;
    private final AirlineRepository airlineRepository;
    private final AirlineMembershipRepository airlineMembershipRepository;

    @Value("${airline.owner-role-id}")
    private Long ownerRoleId;

    // ---------- Applicant side ----------

    @Override
    @Transactional
    public OnboardingApplicationResponse createDraft(OnboardingApplicationRequest request, Long applicantUserId) {
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

        requireCompleteForSubmission(application);

        application.setStatus(OnboardingStatus.SUBMITTED);
        application.setSubmittedAt(Instant.now());
        return OnboardingMapper.toResponse(applicationRepository.save(application));
    }

    // ---------- Review side ----------

    @Override
    @Transactional(readOnly = true)
    public Page<OnboardingApplicationResponse> getApplicationsForReview(Pageable pageable) {
        return applicationRepository.findByStatus(OnboardingStatus.SUBMITTED, pageable)
                .map(OnboardingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OnboardingApplicationResponse getApplicationForReview(Long applicationId) {
        return OnboardingMapper.toResponse(getApplicationOrThrow(applicationId));
    }

    @Override
    @Transactional
    public OnboardingApplicationResponse reviewApplication(Long applicationId, OnboardingReviewRequest request, Long reviewerUserId) {
        AirlineOnboardingApplication application = getApplicationOrThrow(applicationId);

        if (!REVIEWABLE_STATUSES.contains(application.getStatus())) {
            throw new UserException(String.format(
                    ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_REVIEWABLE, applicationId, application.getStatus()));
        }

        ReviewDecision decision = request.getDecision();

        switch (decision) {
            case APPROVED -> approve(application, request.getComments());
            case REJECTED -> {
                application.setStatus(OnboardingStatus.REJECTED);
                application.setRejectionReason(request.getComments());
            }
            case REQUESTED_CHANGES -> {
                application.setStatus(OnboardingStatus.DRAFT);
                application.setRejectionReason(request.getComments());
            }
        }

        application.setReviewedAt(Instant.now());
        AirlineOnboardingApplication saved = applicationRepository.save(application);

        OnboardingReview review = OnboardingReview.builder()
                .application(saved)
                .reviewerUserId(reviewerUserId)
                .decision(decision)
                .comments(request.getComments())
                .build();
        reviewRepository.save(review);

        return OnboardingMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnboardingReviewResponse> getReviewHistory(Long applicationId) {
        getApplicationOrThrow(applicationId);
        return OnboardingMapper.toReviewResponseList(
                reviewRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId));
    }

    // ---------- Helpers ----------

    private void approve(AirlineOnboardingApplication application, String comments) {
        if (application.getInitialAdminUserId() == null) {
            throw new UserException(ErrorMessageUtil.ONBOARDING_INITIAL_ADMIN_REQUIRED_FOR_APPROVAL);
        }

        Airline airline = airlineRepository.save(OnboardingMapper.toAirlineEntity(application));

        AirlineMembership ownerMembership = AirlineMembership.builder()
                .airline(airline)
                .userId(application.getInitialAdminUserId())
                .roleId(ownerRoleId)
                .status(MembershipStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();
        airlineMembershipRepository.save(ownerMembership);

        application.setStatus(OnboardingStatus.APPROVED);
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
