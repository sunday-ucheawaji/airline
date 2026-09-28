package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.ReviewDecision;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.enums.CaseOwnerAction;
import com.sunday.services.enums.InformationRequestStatus;
import com.sunday.services.enums.OnboardingStage;
import com.sunday.services.enums.OnboardingStatus;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingCaseOwnerHistory;
import com.sunday.services.model.OnboardingInformationRequest;
import com.sunday.services.model.OnboardingReview;
import com.sunday.services.repository.AirlineOnboardingApplicationRepository;
import com.sunday.services.repository.OnboardingCaseOwnerHistoryRepository;
import com.sunday.services.repository.OnboardingInformationRequestRepository;
import com.sunday.services.repository.OnboardingReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** Lookups, guards and history writing shared by the onboarding lifecycle and workflow services. */
@Component
@RequiredArgsConstructor
public class OnboardingSupport {

    private final AirlineOnboardingApplicationRepository applicationRepository;
    private final OnboardingReviewRepository reviewRepository;
    private final OnboardingCaseOwnerHistoryRepository caseOwnerHistoryRepository;
    private final OnboardingInformationRequestRepository informationRequestRepository;

    public AirlineOnboardingApplication getOrThrow(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_FOUND_BY_ID, applicationId)));
    }

    /** Staff can never see a draft, even by id: it is the applicant's private working copy. */
    public AirlineOnboardingApplication getVisibleOrThrow(Long applicationId) {
        AirlineOnboardingApplication application = getOrThrow(applicationId);
        if (application.getStatus() == OnboardingStatus.DRAFT) {
            throw new ResourceNotFoundException(
                    String.format(ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_FOUND_BY_ID, applicationId));
        }
        return application;
    }

    /** Throws 409 with {@code message} (formatted with the application id and its status) unless the status is allowed. */
    public void requireStatus(AirlineOnboardingApplication application, Set<OnboardingStatus> allowed, String message) {
        if (!allowed.contains(application.getStatus())) {
            throw new ConflictException(String.format(message, application.getId(), application.getStatus()));
        }
    }

    public void requireOwnedByApplicant(AirlineOnboardingApplication application, Long applicantUserId) {
        if (!application.getApplicantUserId().equals(applicantUserId)) {
            throw new OperationNotPermittedException(String.format(
                    ErrorMessageUtil.ONBOARDING_APPLICATION_NOT_OWNED_BY_APPLICANT, application.getId()));
        }
    }

    public void requireNotApplicantOrNominee(AirlineOnboardingApplication application, Long actorUserId) {
        if (actorUserId.equals(application.getApplicantUserId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_SEGREGATION_OF_DUTIES, "submitted"));
        }
        if (actorUserId.equals(application.getInitialAdminUserId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_OWNER_IS_REVIEWER, application.getId()));
        }
    }

    /** 409 if nobody has claimed the case yet, 403 if the actor is not the case owner. */
    public void requireCaseOwner(AirlineOnboardingApplication application, Long actorUserId) {
        if (application.getCaseOwnerUserId() == null) {
            throw new ConflictException(String.format(ErrorMessageUtil.ONBOARDING_CASE_NOT_CLAIMED, application.getId()));
        }
        if (!application.getCaseOwnerUserId().equals(actorUserId)) {
            throw new OperationNotPermittedException(String.format(ErrorMessageUtil.ONBOARDING_NOT_CASE_OWNER, application.getId()));
        }
    }

    /** True when the user is the applicant or the nominated airline owner of the application. */
    public boolean isParty(AirlineOnboardingApplication application, Long userId) {
        return userId.equals(application.getApplicantUserId()) || userId.equals(application.getInitialAdminUserId());
    }

    public OnboardingReview record(AirlineOnboardingApplication application, Long actorUserId, ReviewDecision decision,
                                   String comments, Long targetUserId, OnboardingStage stage) {
        return reviewRepository.save(OnboardingReview.builder()
                .application(application)
                .actorUserId(actorUserId)
                .decision(decision)
                .comments(comments)
                .targetUserId(targetUserId)
                .stage(stage)
                .build());
    }

    public void recordOwnerChange(AirlineOnboardingApplication application, Long fromUserId, Long toUserId,
                                  Long actorUserId, CaseOwnerAction action, String reason) {
        caseOwnerHistoryRepository.save(OnboardingCaseOwnerHistory.builder()
                .application(application)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .actorUserId(actorUserId)
                .action(action)
                .reason(reason)
                .build());
    }

    public void cancelOpenInformationRequests(AirlineOnboardingApplication application) {
        List<OnboardingInformationRequest> open = informationRequestRepository
                .findByApplicationIdAndStatus(application.getId(), InformationRequestStatus.OPEN);
        open.forEach(request -> request.setStatus(InformationRequestStatus.CANCELLED));
        informationRequestRepository.saveAll(open);
    }
}
