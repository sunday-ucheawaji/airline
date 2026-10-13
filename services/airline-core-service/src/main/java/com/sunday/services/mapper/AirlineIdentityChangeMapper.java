package com.sunday.services.mapper;

import com.sunday.common_lib.payload.response.AirlineIdentityChangeResponse;
import com.sunday.services.model.Airline;
import com.sunday.services.model.AirlineIdentityChangeRequest;

public class AirlineIdentityChangeMapper {

    public static AirlineIdentityChangeResponse toResponse(AirlineIdentityChangeRequest request) {
        Airline airline = request.getAirline();
        return AirlineIdentityChangeResponse.builder()
                .id(request.getId())
                .airlineId(airline.getId())
                .requestedByUserId(request.getRequestedByUserId())
                .currentLegalName(airline.getLegalName())
                .currentRegistrationNumber(airline.getRegistrationNumber())
                .currentIataCode(airline.getIataCode())
                .currentIcaoCode(airline.getIcaoCode())
                .proposedLegalName(request.getProposedLegalName())
                .proposedRegistrationNumber(request.getProposedRegistrationNumber())
                .proposedIataCode(request.getProposedIataCode())
                .proposedIcaoCode(request.getProposedIcaoCode())
                .reason(request.getReason())
                .status(request.getStatus().name())
                .reviewedByUserId(request.getReviewedByUserId())
                .reviewComment(request.getReviewComment())
                .reviewedAt(request.getReviewedAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
