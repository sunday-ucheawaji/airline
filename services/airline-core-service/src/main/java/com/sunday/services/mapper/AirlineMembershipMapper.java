package com.sunday.services.mapper;

import com.sunday.common_lib.payload.response.AirlineMembershipResponse;
import com.sunday.services.model.AirlineMembership;

import java.util.List;

public class AirlineMembershipMapper {

    public static AirlineMembershipResponse toResponse(AirlineMembership membership) {
        if (membership == null) return null;

        return AirlineMembershipResponse.builder()
                .id(membership.getId())
                .airlineId(membership.getAirline().getId())
                .userId(membership.getUserId())
                .roleId(membership.getRoleId())
                .status(membership.getStatus().name())
                .joinedAt(membership.getJoinedAt())
                .createdAt(membership.getCreatedAt())
                .updatedAt(membership.getUpdatedAt())
                .build();
    }

    public static List<AirlineMembershipResponse> toResponseList(List<AirlineMembership> memberships) {
        return memberships.stream().map(AirlineMembershipMapper::toResponse).toList();
    }
}
