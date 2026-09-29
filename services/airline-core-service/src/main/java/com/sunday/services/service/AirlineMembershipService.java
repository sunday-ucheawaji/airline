package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AirlineInvitationRequest;
import com.sunday.common_lib.payload.request.MemberRoleUpdateRequest;
import com.sunday.common_lib.payload.request.MemberStatusUpdateRequest;
import com.sunday.common_lib.payload.response.AirlineMembershipResponse;

import java.util.List;

public interface AirlineMembershipService {

    AirlineMembershipResponse invite(Long airlineId, AirlineInvitationRequest request, Long actorUserId);

    List<AirlineMembershipResponse> listMembers(Long airlineId, Long actorUserId);

    AirlineMembershipResponse accept(Long airlineId, Long membershipId, Long actorUserId);

    AirlineMembershipResponse updateRole(Long airlineId, Long membershipId, MemberRoleUpdateRequest request, Long actorUserId);

    AirlineMembershipResponse updateStatus(Long airlineId, Long membershipId, MemberStatusUpdateRequest request, Long actorUserId);

    /** Revokes a still-PENDING invite, or removes an ACTIVE/SUSPENDED member, depending on the row's current status. */
    void removeOrRevoke(Long airlineId, Long membershipId, Long actorUserId);
}
