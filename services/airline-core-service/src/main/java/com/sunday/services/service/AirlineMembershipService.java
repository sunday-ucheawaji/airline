package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AirlineInvitationRequest;
import com.sunday.common_lib.payload.request.MemberRoleUpdateRequest;
import com.sunday.common_lib.payload.request.MemberStatusUpdateRequest;
import com.sunday.common_lib.payload.response.AirlineMembershipResponse;

import java.util.UUID;
import java.util.List;

public interface AirlineMembershipService {

    /** True when the user has a membership that is invited, active or suspended. */
    boolean hasMembership(UUID userId);

    AirlineMembershipResponse invite(UUID airlineId, AirlineInvitationRequest request, UUID actorUserId);

    List<AirlineMembershipResponse> listMembers(UUID airlineId, UUID actorUserId);

    AirlineMembershipResponse accept(UUID airlineId, UUID membershipId, UUID actorUserId);

    AirlineMembershipResponse updateRole(UUID airlineId, UUID membershipId, MemberRoleUpdateRequest request, UUID actorUserId);

    AirlineMembershipResponse updateStatus(UUID airlineId, UUID membershipId, MemberStatusUpdateRequest request, UUID actorUserId);

    /** Revokes a still-PENDING invite, or removes an ACTIVE/SUSPENDED member, depending on the row's current status. */
    void removeOrRevoke(UUID airlineId, UUID membershipId, UUID actorUserId);
}
