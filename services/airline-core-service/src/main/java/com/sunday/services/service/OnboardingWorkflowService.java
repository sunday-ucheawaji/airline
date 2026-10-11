package com.sunday.services.service;

import java.util.UUID;
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
    OnboardingApplicationResponse claim(UUID applicationId, UUID actorUserId);
    OnboardingApplicationResponse transfer(UUID applicationId, UUID actorUserId, CaseOwnerChangeRequest request);
    OnboardingApplicationResponse release(UUID applicationId, UUID actorUserId, String reason);
    OnboardingApplicationResponse takeOver(UUID applicationId, UUID actorUserId, Collection<String> actorRoles, CaseOwnerChangeRequest request);
    List<CaseOwnerHistoryResponse> getCaseOwnerHistory(UUID applicationId);
    /** Officers when {@code stage} is null, otherwise the specialists holding that stage's role. */
    List<ReviewerResponse> getReviewers(OnboardingStage stage);

    // ----- Stages -----
    OnboardingStageResponse assignStage(UUID applicationId, OnboardingStage stage, UUID actorUserId, StageAssigneeRequest request);
    OnboardingStageResponse decideStage(UUID applicationId, OnboardingStage stage, boolean approve, UUID actorUserId, String comments);
    List<OnboardingStageResponse> getStages(UUID applicationId);

    // ----- Information requests, comments -----
    /** {@code stage} null means the case owner is asking, on behalf of the whole review. */
    InformationRequestResponse requestInformation(UUID applicationId, OnboardingStage stage, UUID actorUserId, String message);
    List<InformationRequestResponse> getInformationRequests(UUID applicationId);
    OnboardingReviewResponse comment(UUID applicationId, UUID actorUserId, String message);

    // ----- Hand-off to final approval -----
    OnboardingApplicationResponse refer(UUID applicationId, UUID actorUserId, String comments);
    OnboardingApplicationResponse sendBack(UUID applicationId, UUID actorUserId, String comments);

    // ----- Applicant side -----
    OnboardingApplicationResponse withdraw(UUID applicationId, UUID applicantUserId);
    List<InformationRequestResponse> getMyInformationRequests(UUID applicationId, UUID applicantUserId);
    InformationRequestResponse respond(UUID applicationId, UUID requestId, UUID applicantUserId, InformationResponseRequest request);
}
