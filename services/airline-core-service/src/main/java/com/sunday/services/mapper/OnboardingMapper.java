package com.sunday.services.mapper;

import com.sunday.common_lib.embeddable.Support;
import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.payload.request.OnboardingApplicationRequest;
import com.sunday.common_lib.payload.response.OnboardingApplicationResponse;
import com.sunday.common_lib.payload.response.CaseOwnerHistoryResponse;
import com.sunday.common_lib.payload.response.InformationRequestResponse;
import com.sunday.common_lib.payload.response.OnboardingReviewResponse;
import com.sunday.common_lib.payload.response.OnboardingStageResponse;
import com.sunday.services.model.Airline;
import com.sunday.services.model.AirlineOnboardingApplication;
import com.sunday.services.model.OnboardingCaseOwnerHistory;
import com.sunday.services.model.OnboardingInformationRequest;
import com.sunday.services.model.OnboardingReview;
import com.sunday.services.model.OnboardingStageReview;

import java.util.List;

public class OnboardingMapper {

    /** Builds the approved Airline from a SUBMITTED/UNDER_REVIEW application — used only at approval time. */
    public static Airline toAirlineEntity(AirlineOnboardingApplication application) {
        Airline airline = Airline.builder()
                .legalName(application.getLegalName())
                .name(application.getDisplayName())
                .registrationNumber(application.getRegistrationNumber())
                .iataCode(application.getIataCode())
                .icaoCode(application.getIcaoCode())
                .country(application.getCountry())
                .logoUrl(application.getLogoUrl())
                .website(application.getWebsite())
                .alliance(application.getAlliance())
                .headquartersCityId(application.getHeadquartersCityId())
                .status(AirlineStatus.ACTIVE)
                .build();

        if (application.getSupportEmail() != null || application.getSupportPhone() != null
                || application.getSupportHours() != null) {
            airline.setSupport(Support.builder()
                    .email(application.getSupportEmail())
                    .phone(application.getSupportPhone())
                    .hours(application.getSupportHours())
                    .build());
        }

        return airline;
    }

    /** Applies only the fields present on the request — used for both create-draft and PATCH. */
    public static void applyToEntity(AirlineOnboardingApplication application, OnboardingApplicationRequest request) {
        if (application == null || request == null) return;

        if (request.getInitialAdminUserId() != null) application.setInitialAdminUserId(request.getInitialAdminUserId());
        if (request.getLegalName() != null) application.setLegalName(request.getLegalName());
        if (request.getDisplayName() != null) application.setDisplayName(request.getDisplayName());
        if (request.getIataCode() != null) application.setIataCode(request.getIataCode());
        if (request.getIcaoCode() != null) application.setIcaoCode(request.getIcaoCode());
        if (request.getCountry() != null) application.setCountry(request.getCountry());
        if (request.getRegistrationNumber() != null) application.setRegistrationNumber(request.getRegistrationNumber());
        if (request.getAlliance() != null) application.setAlliance(request.getAlliance());
        if (request.getWebsite() != null) application.setWebsite(request.getWebsite());
        if (request.getLogoUrl() != null) application.setLogoUrl(request.getLogoUrl());
        if (request.getSupportEmail() != null) application.setSupportEmail(request.getSupportEmail());
        if (request.getSupportPhone() != null) application.setSupportPhone(request.getSupportPhone());
        if (request.getSupportHours() != null) application.setSupportHours(request.getSupportHours());
        if (request.getHeadquartersCityId() != null) application.setHeadquartersCityId(request.getHeadquartersCityId());
    }

    public static OnboardingApplicationResponse toResponse(AirlineOnboardingApplication application) {
        if (application == null) return null;

        return OnboardingApplicationResponse.builder()
                .id(application.getId())
                .applicantUserId(application.getApplicantUserId())
                .initialAdminUserId(application.getInitialAdminUserId())
                .legalName(application.getLegalName())
                .displayName(application.getDisplayName())
                .iataCode(application.getIataCode())
                .icaoCode(application.getIcaoCode())
                .country(application.getCountry())
                .registrationNumber(application.getRegistrationNumber())
                .alliance(application.getAlliance())
                .website(application.getWebsite())
                .logoUrl(application.getLogoUrl())
                .supportEmail(application.getSupportEmail())
                .supportPhone(application.getSupportPhone())
                .supportHours(application.getSupportHours())
                .headquartersCityId(application.getHeadquartersCityId())
                .status(application.getStatus().name())
                .rejectionReason(application.getRejectionReason())
                .airlineId(application.getAirlineId())
                .caseOwnerUserId(application.getCaseOwnerUserId())
                .submittedAt(application.getSubmittedAt())
                .reviewedAt(application.getReviewedAt())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    public static List<OnboardingApplicationResponse> toResponseList(List<AirlineOnboardingApplication> applications) {
        return applications.stream().map(OnboardingMapper::toResponse).toList();
    }

    public static OnboardingReviewResponse toReviewResponse(OnboardingReview review) {
        if (review == null) return null;

        return OnboardingReviewResponse.builder()
                .id(review.getId())
                .applicationId(review.getApplication().getId())
                .actorUserId(review.getActorUserId())
                .decision(review.getDecision().name())
                .comments(review.getComments())
                .targetUserId(review.getTargetUserId())
                .stage(review.getStage() == null ? null : review.getStage().name())
                .createdAt(review.getCreatedAt())
                .build();
    }

    public static List<OnboardingReviewResponse> toReviewResponseList(List<OnboardingReview> reviews) {
        return reviews.stream().map(OnboardingMapper::toReviewResponse).toList();
    }

    public static OnboardingStageResponse toStageResponse(OnboardingStageReview stage) {
        return OnboardingStageResponse.builder()
                .applicationId(stage.getApplication().getId())
                .stage(stage.getStage().name())
                .status(stage.getStatus().name())
                .assigneeUserId(stage.getAssigneeUserId())
                .comments(stage.getComments())
                .decidedAt(stage.getDecidedAt())
                .build();
    }

    public static InformationRequestResponse toInformationRequestResponse(OnboardingInformationRequest request) {
        return InformationRequestResponse.builder()
                .id(request.getId())
                .applicationId(request.getApplication().getId())
                .stage(request.getStage() == null ? null : request.getStage().name())
                .requestedByUserId(request.getRequestedByUserId())
                .message(request.getMessage())
                .status(request.getStatus().name())
                .response(request.getResponse())
                .createdAt(request.getCreatedAt())
                .respondedAt(request.getRespondedAt())
                .build();
    }

    public static CaseOwnerHistoryResponse toCaseOwnerHistoryResponse(OnboardingCaseOwnerHistory entry) {
        return CaseOwnerHistoryResponse.builder()
                .id(entry.getId())
                .applicationId(entry.getApplication().getId())
                .fromUserId(entry.getFromUserId())
                .toUserId(entry.getToUserId())
                .actorUserId(entry.getActorUserId())
                .action(entry.getAction().name())
                .reason(entry.getReason())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
