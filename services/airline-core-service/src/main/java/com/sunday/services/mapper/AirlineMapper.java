package com.sunday.services.mapper;

import com.sunday.common_lib.embeddable.Support;
import com.sunday.common_lib.payload.request.AirlineRequest;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.services.model.Airline;

public class AirlineMapper {

    // Airlines are only ever created via OnboardingMapper.toAirlineEntity at
    // approval time — there is no direct AirlineRequest -> new Airline path.

    public static AirlineResponse toResponse(Airline airline) {
        if (airline == null) return null;

        return AirlineResponse.builder()
                .id(airline.getId())
                .legalName(airline.getLegalName())
                .name(airline.getName())
                .alias(airline.getAlias())
                .registrationNumber(airline.getRegistrationNumber())
                .iataCode(airline.getIataCode())
                .icaoCode(airline.getIcaoCode())
                .country(airline.getCountry())
                .logoUrl(airline.getLogoUrl())
                .website(airline.getWebsite())
                .status(airline.getStatus())
                .alliance(airline.getAlliance())
                .support(airline.getSupport())
                .headquartersCityId(airline.getHeadquartersCityId())
                .createdAt(airline.getCreatedAt())
                .updatedAt(airline.getUpdatedAt())
                .build();
    }

    public static void updateEntity(Airline airline, AirlineRequest request) {
        if (airline == null || request == null) return;

        airline.setLegalName(request.getLegalName());
        airline.setName(request.getName());
        airline.setAlias(request.getAlias());
        airline.setRegistrationNumber(request.getRegistrationNumber());
        airline.setIataCode(request.getIataCode());
        airline.setIcaoCode(request.getIcaoCode());
        airline.setCountry(request.getCountry());
        airline.setLogoUrl(request.getLogoUrl());
        airline.setWebsite(request.getWebsite());
        // status is deliberately not settable here — it only changes via the
        // dedicated /approve, /suspend, /ban admin endpoints (changeStatusByAdmin).
        airline.setAlliance(request.getAlliance());
        airline.setHeadquartersCityId(request.getHeadquartersCityId());

        if (airline.getSupport() == null) {
            airline.setSupport(new Support());
        }
        airline.getSupport().setEmail(request.getSupportEmail());
        airline.getSupport().setPhone(request.getSupportPhone());
        airline.getSupport().setHours(request.getSupportHours());
    }
}
