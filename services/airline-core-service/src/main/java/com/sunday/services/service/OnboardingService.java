package com.sunday.services.service;

import com.sunday.common_lib.payload.request.OnboardingApplicationRequest;
import com.sunday.common_lib.payload.request.OnboardingReviewRequest;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.OnboardingReviewResponse;
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

    // ----- Review side -----
    Page<OnboardingApplicationResponse> getApplicationsForReview(Pageable pageable);
    OnboardingApplicationResponse getApplicationForReview(Long applicationId);
    OnboardingApplicationResponse reviewApplication(Long applicationId, OnboardingReviewRequest request, Long reviewerUserId);
    List<OnboardingReviewResponse> getReviewHistory(Long applicationId);
}
