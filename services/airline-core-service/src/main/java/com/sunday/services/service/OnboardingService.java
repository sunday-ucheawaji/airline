package com.sunday.services.service;

import com.sunday.common_lib.payload.request.OnboardingApplicationRequest;
import com.sunday.common_lib.payload.request.OwnerAssignmentRequest;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.OnboardingReviewResponse;
import com.sunday.services.enums.OnboardingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OnboardingService {

    // ----- Applicant side -----
    OnboardingApplicationResponse createDraft(OnboardingApplicationRequest request, Long applicantUserId);
    OnboardingApplicationResponse updateDraft(Long applicationId, OnboardingApplicationRequest request, Long applicantUserId);
    OnboardingApplicationResponse getMyApplicationById(Long applicationId, Long applicantUserId);
    List<OnboardingApplicationResponse> getMyApplications(Long applicantUserId);
    OnboardingApplicationResponse submitApplication(Long applicationId, Long applicantUserId);

    // ----- Staff side (drafts are never visible here) -----
    Page<OnboardingApplicationResponse> getApplicationsForReview(OnboardingStatus status, Pageable pageable);
    OnboardingApplicationResponse getApplicationForReview(Long applicationId);
    List<OnboardingReviewResponse> getReviewHistory(Long applicationId);

    OnboardingApplicationResponse returnApplication(Long applicationId, String comments, Long actorUserId);
    OnboardingApplicationResponse approveApplication(Long applicationId, String comments, Long actorUserId);
    OnboardingApplicationResponse rejectApplication(Long applicationId, String comments, Long actorUserId);
    OnboardingApplicationResponse assignOwner(Long applicationId, OwnerAssignmentRequest request, Long actorUserId);
    OnboardingApplicationResponse provisionApplication(Long applicationId, String comments, Long actorUserId);
}
