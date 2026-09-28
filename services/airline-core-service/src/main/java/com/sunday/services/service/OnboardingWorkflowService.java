package com.sunday.services.service;

import com.sunday.common_lib.payload.request.CaseOwnerChangeRequest;
import com.sunday.common_lib.payload.request.InformationRequestCreateRequest;
import com.sunday.common_lib.payload.request.InformationResponseRequest;
import com.sunday.common_lib.payload.request.StageAssigneeRequest;
import com.sunday.common_lib.payload.response.CaseOwnerHistoryResponse;
import com.sunday.common_lib.payload.response.InformationRequestResponse;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.OnboardingReviewResponse;
import com.sunday.common_lib.payload.response.OnboardingStageResponse;
import com.sunday.common_lib.payload.response.ReviewerResponse;
import com.sunday.services.enums.OnboardingStage;

import java.util.Collection;
import java.util.List;

/** The staged review: case ownership, specialist stages, information requests, referral and withdrawal. */
public interface OnboardingWorkflowService {

    // ----- Case ownership (onboarding officer) -----
    OnboardingApplicationResponse claim(Long applicationId, Long actorUserId);
    OnboardingApplicationResponse transfer(Long applicationId, Long actorUserId, CaseOwnerChangeRequest request);
    OnboardingApplicationResponse release(Long applicationId, Long actorUserId, String reason);
    OnboardingApplicationResponse takeOver(Long applicationId, Long actorUserId, Collection<String> actorRoles, CaseOwnerChangeRequest request);
    List<CaseOwnerHistoryResponse> getCaseOwnerHistory(Long applicationId);
    /** Officers when {@code stage} is null, otherwise the specialists holding that stage's role. */
    List<ReviewerResponse> getReviewers(OnboardingStage stage);

    // ----- Stages -----
    OnboardingStageResponse assignStage(Long applicationId, OnboardingStage stage, Long actorUserId, StageAssigneeRequest request);
    OnboardingStageResponse decideStage(Long applicationId, OnboardingStage stage, boolean approve, Long actorUserId, String comments);
    List<OnboardingStageResponse> getStages(Long applicationId);

    // ----- Information requests, comments -----
    /** {@code stage} null means the case owner is asking, on behalf of the whole review. */
    InformationRequestResponse requestInformation(Long applicationId, OnboardingStage stage, Long actorUserId, String message);
    List<InformationRequestResponse> getInformationRequests(Long applicationId);
    OnboardingReviewResponse comment(Long applicationId, Long actorUserId, String message);

    // ----- Hand-off to final approval -----
    OnboardingApplicationResponse refer(Long applicationId, Long actorUserId, String comments);
    OnboardingApplicationResponse sendBack(Long applicationId, Long actorUserId, String comments);

    // ----- Applicant side -----
    OnboardingApplicationResponse withdraw(Long applicationId, Long applicantUserId);
    List<InformationRequestResponse> getMyInformationRequests(Long applicationId, Long applicantUserId);
    InformationRequestResponse respond(Long applicationId, Long requestId, Long applicantUserId, InformationResponseRequest request);
}
