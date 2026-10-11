package com.sunday.services.service;

import java.util.UUID;
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
    OnboardingApplicationResponse createDraft(OnboardingApplicationRequest request, UUID applicantUserId);
    OnboardingApplicationResponse updateDraft(UUID applicationId, OnboardingApplicationRequest request, UUID applicantUserId);
    OnboardingApplicationResponse getMyApplicationById(UUID applicationId, UUID applicantUserId);
    List<OnboardingApplicationResponse> getMyApplications(UUID applicantUserId);
    OnboardingApplicationResponse submitApplication(UUID applicationId, UUID applicantUserId);

    // ----- Staff side (drafts are never visible here) -----
    Page<OnboardingApplicationResponse> getApplicationsForReview(OnboardingStatus status, UUID caseOwnerUserId, Pageable pageable);
    OnboardingApplicationResponse getApplicationForReview(UUID applicationId);
    List<OnboardingReviewResponse> getReviewHistory(UUID applicationId);

    OnboardingApplicationResponse returnApplication(UUID applicationId, String comments, UUID actorUserId);
    OnboardingApplicationResponse approveApplication(UUID applicationId, String comments, UUID actorUserId);
    OnboardingApplicationResponse rejectApplication(UUID applicationId, String comments, UUID actorUserId);
    OnboardingApplicationResponse assignOwner(UUID applicationId, OwnerAssignmentRequest request, UUID actorUserId);
    OnboardingApplicationResponse provisionApplication(UUID applicationId, String comments, UUID actorUserId);
}
