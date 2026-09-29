package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.AirlineInvitationRequest;
import com.sunday.common_lib.payload.request.MemberRoleUpdateRequest;
import com.sunday.common_lib.payload.request.MemberStatusUpdateRequest;
import com.sunday.common_lib.payload.response.AirlineMembershipResponse;
import com.sunday.common_lib.payload.response.ReviewerResponse;
import com.sunday.services.client.UserClient;
import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.model.Airline;
import com.sunday.services.model.AirlineMembership;
import com.sunday.services.repository.AirlineMembershipRepository;
import com.sunday.services.repository.AirlineRepository;
import com.sunday.services.service.AirlineService;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AirlineMembershipServiceImplTest {

    private static final long AIRLINE_ID = 1L;
    private static final long ACTOR = 10L; // an OWNER/ADMIN doing the inviting/managing
    private static final long INVITEE = 20L;
    private static final long ADMIN_ROLE = 4L;
    private static final long VIEWER_ROLE = 5L;
    private static final long OWNER_ROLE = 3L;

    @Mock AirlineMembershipRepository membershipRepository;
    @Mock AirlineRepository airlineRepository;
    @Mock AirlineService airlineService;
    @Mock UserClient userClient;
    @Mock PlatformUserGuard platformUserGuard;

    private AirlineMembershipServiceImpl service;
    private Airline airline;

    @BeforeEach
    void setUp() {
        service = new AirlineMembershipServiceImpl(membershipRepository, airlineRepository, airlineService, userClient, platformUserGuard);
        ReflectionTestUtils.setField(service, "adminRoleId", ADMIN_ROLE);
        ReflectionTestUtils.setField(service, "viewerRoleId", VIEWER_ROLE);
        ReflectionTestUtils.setField(service, "ownerRoleId", OWNER_ROLE);
        ReflectionTestUtils.setField(service, "invitationExpiry", Duration.ofDays(7));

        airline = Airline.builder().id(AIRLINE_ID).build();
        when(airlineRepository.findById(AIRLINE_ID)).thenReturn(Optional.of(airline));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private AirlineInvitationRequest inviteRequest(long roleId) {
        return AirlineInvitationRequest.builder().email("invitee@example.com").roleId(roleId).build();
    }

    private AirlineMembership membership(long id, MembershipStatus status, long roleId) {
        return AirlineMembership.builder().id(id).airline(airline).userId(INVITEE).roleId(roleId).status(status).build();
    }

    private FeignException.NotFound notFound() {
        Request request = Request.create(Request.HttpMethod.GET, "/internal/users/by-email/x",
                java.util.Map.of(), null, new RequestTemplate());
        return new FeignException.NotFound("not found", request, null, null);
    }

    // ---------- invite ----------

    @Test
    void invite_createsPendingMembership() {
        when(membershipRepository.findByUserIdAndAirlineId(INVITEE, AIRLINE_ID)).thenReturn(Optional.empty());
        when(userClient.getUserByEmail("invitee@example.com"))
                .thenReturn(ReviewerResponse.builder().userId(INVITEE).build());

        AirlineMembershipResponse response = service.invite(AIRLINE_ID, inviteRequest(ADMIN_ROLE), ACTOR);

        assertThat(response.getStatus()).isEqualTo("INVITED");
        assertThat(response.getRoleId()).isEqualTo(ADMIN_ROLE);
        assertThat(response.getInvitedByUserId()).isEqualTo(ACTOR);
        assertThat(response.getExpiresAt()).isAfter(Instant.now());
        verify(airlineService).requirePermission(ACTOR, List.of(AIRLINE_ID), "MEMBER_INVITE");
        verify(platformUserGuard).requireNotPlatformUser(eq(INVITEE), any());
    }

    @Test
    void invite_targetRoleOwner_rejected() {
        assertThatThrownBy(() -> service.invite(AIRLINE_ID, inviteRequest(OWNER_ROLE), ACTOR))
                .isInstanceOf(BadRequestException.class);
        verify(userClient, never()).getUserByEmail(any());
    }

    @Test
    void invite_alreadyActiveMember_rejected() {
        when(membershipRepository.findByUserIdAndAirlineId(INVITEE, AIRLINE_ID))
                .thenReturn(Optional.of(membership(1L, MembershipStatus.ACTIVE, VIEWER_ROLE)));
        when(userClient.getUserByEmail(any())).thenReturn(ReviewerResponse.builder().userId(INVITEE).build());

        assertThatThrownBy(() -> service.invite(AIRLINE_ID, inviteRequest(ADMIN_ROLE), ACTOR))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void invite_alreadyPending_rejected() {
        when(membershipRepository.findByUserIdAndAirlineId(INVITEE, AIRLINE_ID))
                .thenReturn(Optional.of(membership(1L, MembershipStatus.INVITED, VIEWER_ROLE)));
        when(userClient.getUserByEmail(any())).thenReturn(ReviewerResponse.builder().userId(INVITEE).build());

        assertThatThrownBy(() -> service.invite(AIRLINE_ID, inviteRequest(ADMIN_ROLE), ACTOR))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void invite_reInvitesAPreviouslyRemovedUser() {
        when(membershipRepository.findByUserIdAndAirlineId(INVITEE, AIRLINE_ID))
                .thenReturn(Optional.of(membership(1L, MembershipStatus.REMOVED, VIEWER_ROLE)));
        when(userClient.getUserByEmail(any())).thenReturn(ReviewerResponse.builder().userId(INVITEE).build());

        AirlineMembershipResponse response = service.invite(AIRLINE_ID, inviteRequest(ADMIN_ROLE), ACTOR);

        assertThat(response.getStatus()).isEqualTo("INVITED");
        assertThat(response.getRoleId()).isEqualTo(ADMIN_ROLE);
    }

    @Test
    void invite_platformStaffTarget_rejected() {
        when(membershipRepository.findByUserIdAndAirlineId(INVITEE, AIRLINE_ID)).thenReturn(Optional.empty());
        when(userClient.getUserByEmail(any())).thenReturn(ReviewerResponse.builder().userId(INVITEE).build());
        doThrow(new ConflictException("staff")).when(platformUserGuard).requireNotPlatformUser(eq(INVITEE), any());

        assertThatThrownBy(() -> service.invite(AIRLINE_ID, inviteRequest(ADMIN_ROLE), ACTOR))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void invite_emailNotFound_rejected() {
        when(userClient.getUserByEmail(any())).thenThrow(notFound());

        assertThatThrownBy(() -> service.invite(AIRLINE_ID, inviteRequest(ADMIN_ROLE), ACTOR))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void invite_userServiceUnavailable_failsClosed() {
        when(userClient.getUserByEmail(any())).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.invite(AIRLINE_ID, inviteRequest(ADMIN_ROLE), ACTOR))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void invite_noPermission_rejected() {
        doThrow(new OperationNotPermittedException("no")).when(airlineService)
                .requirePermission(eq(ACTOR), anyList(), eq("MEMBER_INVITE"));

        assertThatThrownBy(() -> service.invite(AIRLINE_ID, inviteRequest(ADMIN_ROLE), ACTOR))
                .isInstanceOf(OperationNotPermittedException.class);
        verify(userClient, never()).getUserByEmail(any());
    }

    // ---------- accept ----------

    @Test
    void accept_success() {
        AirlineMembership pending = membership(5L, MembershipStatus.INVITED, ADMIN_ROLE);
        pending.setExpiresAt(Instant.now().plusSeconds(3600));
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(pending));

        AirlineMembershipResponse response = service.accept(AIRLINE_ID, 5L, INVITEE);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(pending.getJoinedAt()).isNotNull();
        assertThat(pending.getExpiresAt()).isNull();
    }

    @Test
    void accept_wrongUser_rejected() {
        AirlineMembership pending = membership(5L, MembershipStatus.INVITED, ADMIN_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.accept(AIRLINE_ID, 5L, ACTOR))
                .isInstanceOf(OperationNotPermittedException.class);
    }

    @Test
    void accept_notPending_rejected() {
        AirlineMembership active = membership(5L, MembershipStatus.ACTIVE, ADMIN_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.accept(AIRLINE_ID, 5L, INVITEE))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void accept_expired_rejectedAndMarkedExpired() {
        AirlineMembership pending = membership(5L, MembershipStatus.INVITED, ADMIN_ROLE);
        pending.setExpiresAt(Instant.now().minusSeconds(1));
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.accept(AIRLINE_ID, 5L, INVITEE))
                .isInstanceOf(ConflictException.class);
        assertThat(pending.getStatus()).isEqualTo(MembershipStatus.EXPIRED);
    }

    @Test
    void accept_wrongAirline_notFound() {
        Airline otherAirline = Airline.builder().id(999L).build();
        AirlineMembership pending = AirlineMembership.builder().id(5L).airline(otherAirline).userId(INVITEE)
                .roleId(ADMIN_ROLE).status(MembershipStatus.INVITED).build();
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.accept(AIRLINE_ID, 5L, INVITEE))
                .isInstanceOf(com.sunday.common_lib.exception.ResourceNotFoundException.class);
    }

    // ---------- updateRole ----------

    @Test
    void updateRole_success() {
        AirlineMembership active = membership(5L, MembershipStatus.ACTIVE, VIEWER_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(active));

        AirlineMembershipResponse response = service.updateRole(
                AIRLINE_ID, 5L, MemberRoleUpdateRequest.builder().roleId(ADMIN_ROLE).build(), ACTOR);

        assertThat(response.getRoleId()).isEqualTo(ADMIN_ROLE);
        verify(airlineService).requirePermission(ACTOR, List.of(AIRLINE_ID), "MEMBER_UPDATE_ROLE");
    }

    @Test
    void updateRole_ownerTarget_rejected() {
        AirlineMembership owner = membership(5L, MembershipStatus.ACTIVE, OWNER_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.updateRole(
                AIRLINE_ID, 5L, MemberRoleUpdateRequest.builder().roleId(VIEWER_ROLE).build(), ACTOR))
                .isInstanceOf(OperationNotPermittedException.class);
    }

    @Test
    void updateRole_newRoleOwner_rejected() {
        AirlineMembership active = membership(5L, MembershipStatus.ACTIVE, VIEWER_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.updateRole(
                AIRLINE_ID, 5L, MemberRoleUpdateRequest.builder().roleId(OWNER_ROLE).build(), ACTOR))
                .isInstanceOf(BadRequestException.class);
    }

    // ---------- updateStatus ----------

    @Test
    void updateStatus_suspendsActiveMember() {
        AirlineMembership active = membership(5L, MembershipStatus.ACTIVE, VIEWER_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(active));

        AirlineMembershipResponse response = service.updateStatus(
                AIRLINE_ID, 5L, MemberStatusUpdateRequest.builder().status("SUSPENDED").build(), ACTOR);

        assertThat(response.getStatus()).isEqualTo("SUSPENDED");
    }

    @Test
    void updateStatus_reactivatesSuspendedMember() {
        AirlineMembership suspended = membership(5L, MembershipStatus.SUSPENDED, VIEWER_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(suspended));

        AirlineMembershipResponse response = service.updateStatus(
                AIRLINE_ID, 5L, MemberStatusUpdateRequest.builder().status("ACTIVE").build(), ACTOR);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void updateStatus_invalidValue_rejected() {
        assertThatThrownBy(() -> service.updateStatus(
                AIRLINE_ID, 5L, MemberStatusUpdateRequest.builder().status("REMOVED").build(), ACTOR))
                .isInstanceOf(BadRequestException.class);
        verify(membershipRepository, never()).findById(any());
    }

    @Test
    void updateStatus_suspendLastActiveOwner_rejected() {
        AirlineMembership owner = membership(5L, MembershipStatus.ACTIVE, OWNER_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(owner));
        when(membershipRepository.countByAirlineIdAndRoleIdAndStatus(AIRLINE_ID, OWNER_ROLE, MembershipStatus.ACTIVE))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.updateStatus(
                AIRLINE_ID, 5L, MemberStatusUpdateRequest.builder().status("SUSPENDED").build(), ACTOR))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- removeOrRevoke ----------

    @Test
    void removeOrRevoke_revokesPendingInvite() {
        AirlineMembership pending = membership(5L, MembershipStatus.INVITED, ADMIN_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(pending));

        service.removeOrRevoke(AIRLINE_ID, 5L, ACTOR);

        assertThat(pending.getStatus()).isEqualTo(MembershipStatus.REVOKED);
        verify(airlineService).requirePermission(ACTOR, List.of(AIRLINE_ID), "MEMBER_INVITE");
    }

    @Test
    void removeOrRevoke_removesActiveMember() {
        AirlineMembership active = membership(5L, MembershipStatus.ACTIVE, VIEWER_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(active));

        service.removeOrRevoke(AIRLINE_ID, 5L, ACTOR);

        assertThat(active.getStatus()).isEqualTo(MembershipStatus.REMOVED);
        verify(airlineService).requirePermission(ACTOR, List.of(AIRLINE_ID), "MEMBER_REMOVE");
    }

    @Test
    void removeOrRevoke_lastActiveOwner_rejected() {
        AirlineMembership owner = membership(5L, MembershipStatus.ACTIVE, OWNER_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(owner));
        when(membershipRepository.countByAirlineIdAndRoleIdAndStatus(AIRLINE_ID, OWNER_ROLE, MembershipStatus.ACTIVE))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.removeOrRevoke(AIRLINE_ID, 5L, ACTOR))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void removeOrRevoke_alreadyRemoved_rejected() {
        AirlineMembership removed = membership(5L, MembershipStatus.REMOVED, VIEWER_ROLE);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(removed));

        assertThatThrownBy(() -> service.removeOrRevoke(AIRLINE_ID, 5L, ACTOR))
                .isInstanceOf(ConflictException.class);
    }

    // ---------- listMembers ----------

    @Test
    void listMembers_requiresReadPermission() {
        when(membershipRepository.findByAirlineId(AIRLINE_ID)).thenReturn(List.of(membership(1L, MembershipStatus.ACTIVE, VIEWER_ROLE)));

        List<AirlineMembershipResponse> result = service.listMembers(AIRLINE_ID, ACTOR);

        assertThat(result).hasSize(1);
        verify(airlineService).requirePermission(ACTOR, List.of(AIRLINE_ID), "MEMBER_READ");
    }
}
