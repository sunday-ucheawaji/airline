package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.AirlineMembershipPermissions;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.AirlineInvitationRequest;
import com.sunday.common_lib.payload.request.MemberRoleUpdateRequest;
import com.sunday.common_lib.payload.request.MemberStatusUpdateRequest;
import com.sunday.common_lib.payload.response.AirlineMembershipResponse;
import com.sunday.common_lib.payload.response.ReviewerResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.client.UserClient;
import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.mapper.AirlineMembershipMapper;
import com.sunday.services.model.Airline;
import com.sunday.services.model.AirlineMembership;
import com.sunday.services.repository.AirlineMembershipRepository;
import com.sunday.services.repository.AirlineRepository;
import com.sunday.services.service.AirlineMembershipService;
import com.sunday.services.service.AirlineService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Invitations, roster and role/status changes for a single airline's team. Reuses {@link AirlineMembership}
 * with {@code status = INVITED} for a pending invite rather than a separate table — that status has existed
 * in the model since it was first built, unused until now.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AirlineMembershipServiceImpl implements AirlineMembershipService {

    private final AirlineMembershipRepository membershipRepository;
    private final AirlineRepository airlineRepository;
    private final AirlineService airlineService;
    private final UserClient userClient;
    private final PlatformUserGuard platformUserGuard;

    @Value("${airline.admin-role-id}")
    private Long adminRoleId;

    @Value("${airline.viewer-role-id}")
    private Long viewerRoleId;

    @Value("${airline.owner-role-id}")
    private Long ownerRoleId;

    @Value("${airline.invitation-expiry:7d}")
    private Duration invitationExpiry;

    @Override
    public AirlineMembershipResponse invite(Long airlineId, AirlineInvitationRequest request, Long actorUserId) {
        airlineService.requirePermission(actorUserId, List.of(airlineId), AirlineMembershipPermissions.INVITE);
        requireInvitableRole(request.getRoleId());
        Airline airline = getAirlineOrThrow(airlineId);

        Long invitedUserId = resolveUserByEmail(request.getEmail());
        platformUserGuard.requireNotPlatformUser(invitedUserId,
                String.format(ErrorMessageUtil.MEMBER_INVITE_TARGET_IS_PLATFORM_STAFF, invitedUserId));

        AirlineMembership membership = membershipRepository.findByUserIdAndAirlineId(invitedUserId, airlineId).orElse(null);
        if (membership != null) {
            if (membership.getStatus() == MembershipStatus.ACTIVE || membership.getStatus() == MembershipStatus.SUSPENDED) {
                throw new ConflictException(String.format(ErrorMessageUtil.MEMBER_ALREADY_ACTIVE, invitedUserId, airlineId));
            }
            if (membership.getStatus() == MembershipStatus.INVITED) {
                throw new ConflictException(String.format(ErrorMessageUtil.MEMBER_ALREADY_INVITED, invitedUserId, airlineId));
            }
        } else {
            membership = AirlineMembership.builder().airline(airline).userId(invitedUserId).build();
        }

        membership.setRoleId(request.getRoleId());
        membership.setStatus(MembershipStatus.INVITED);
        membership.setInvitedByUserId(actorUserId);
        membership.setExpiresAt(Instant.now().plus(invitationExpiry));
        membership.setJoinedAt(null);
        return AirlineMembershipMapper.toResponse(membershipRepository.save(membership));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AirlineMembershipResponse> listMembers(Long airlineId, Long actorUserId) {
        airlineService.requirePermission(actorUserId, List.of(airlineId), AirlineMembershipPermissions.READ);
        return AirlineMembershipMapper.toResponseList(membershipRepository.findByAirlineId(airlineId));
    }

    @Override
    @CacheEvict(cacheNames = "airlinesByUser", allEntries = true)
    public AirlineMembershipResponse accept(Long airlineId, Long membershipId, Long actorUserId) {
        AirlineMembership membership = getMembershipOrThrow(airlineId, membershipId);
        if (!membership.getUserId().equals(actorUserId)) {
            throw new OperationNotPermittedException(ErrorMessageUtil.MEMBERSHIP_ACCEPT_NOT_INVITEE);
        }
        if (membership.getStatus() != MembershipStatus.INVITED) {
            throw new ConflictException(String.format(ErrorMessageUtil.MEMBERSHIP_NOT_PENDING, membershipId));
        }
        if (membership.getExpiresAt() != null && membership.getExpiresAt().isBefore(Instant.now())) {
            membership.setStatus(MembershipStatus.EXPIRED);
            membershipRepository.save(membership);
            throw new ConflictException(ErrorMessageUtil.MEMBERSHIP_INVITATION_EXPIRED);
        }

        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setJoinedAt(Instant.now());
        membership.setExpiresAt(null);
        return AirlineMembershipMapper.toResponse(membershipRepository.save(membership));
    }

    @Override
    public AirlineMembershipResponse updateRole(Long airlineId, Long membershipId, MemberRoleUpdateRequest request, Long actorUserId) {
        airlineService.requirePermission(actorUserId, List.of(airlineId), AirlineMembershipPermissions.UPDATE_ROLE);
        requireInvitableRole(request.getRoleId());
        AirlineMembership membership = getMembershipOrThrow(airlineId, membershipId);
        if (ownerRoleId.equals(membership.getRoleId())) {
            throw new OperationNotPermittedException(ErrorMessageUtil.MEMBER_ROLE_UPDATE_OWNER_LOCKED);
        }
        if (membership.getStatus() != MembershipStatus.ACTIVE && membership.getStatus() != MembershipStatus.SUSPENDED) {
            throw new ConflictException(String.format(ErrorMessageUtil.MEMBERSHIP_NOT_ACTIVE_OR_SUSPENDED, membershipId));
        }

        membership.setRoleId(request.getRoleId());
        return AirlineMembershipMapper.toResponse(membershipRepository.save(membership));
    }

    @Override
    @CacheEvict(cacheNames = "airlinesByUser", allEntries = true)
    public AirlineMembershipResponse updateStatus(Long airlineId, Long membershipId, MemberStatusUpdateRequest request, Long actorUserId) {
        airlineService.requirePermission(actorUserId, List.of(airlineId), AirlineMembershipPermissions.UPDATE_STATUS);
        MembershipStatus target = parseActivatableStatus(request.getStatus());
        AirlineMembership membership = getMembershipOrThrow(airlineId, membershipId);
        if (membership.getStatus() != MembershipStatus.ACTIVE && membership.getStatus() != MembershipStatus.SUSPENDED) {
            throw new ConflictException(String.format(ErrorMessageUtil.MEMBERSHIP_NOT_ACTIVE_OR_SUSPENDED, membershipId));
        }
        if (target == MembershipStatus.SUSPENDED) {
            requireNotLastActiveOwner(membership);
        }

        membership.setStatus(target);
        return AirlineMembershipMapper.toResponse(membershipRepository.save(membership));
    }

    @Override
    @CacheEvict(cacheNames = "airlinesByUser", allEntries = true)
    public void removeOrRevoke(Long airlineId, Long membershipId, Long actorUserId) {
        AirlineMembership membership = getMembershipOrThrow(airlineId, membershipId);
        if (membership.getStatus() == MembershipStatus.INVITED) {
            airlineService.requirePermission(actorUserId, List.of(airlineId), AirlineMembershipPermissions.INVITE);
            membership.setStatus(MembershipStatus.REVOKED);
        } else if (membership.getStatus() == MembershipStatus.ACTIVE || membership.getStatus() == MembershipStatus.SUSPENDED) {
            airlineService.requirePermission(actorUserId, List.of(airlineId), AirlineMembershipPermissions.REMOVE);
            requireNotLastActiveOwner(membership);
            membership.setStatus(MembershipStatus.REMOVED);
        } else {
            throw new ConflictException(String.format(ErrorMessageUtil.MEMBERSHIP_NOT_PENDING, membershipId));
        }
        membershipRepository.save(membership);
    }

    // ---------- Helpers ----------

    private Airline getAirlineOrThrow(Long airlineId) {
        return airlineRepository.findById(airlineId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRLINE_NOT_FOUND_BY_ID, airlineId)));
    }

    private AirlineMembership getMembershipOrThrow(Long airlineId, Long membershipId) {
        AirlineMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.MEMBERSHIP_NOT_FOUND, membershipId, airlineId)));
        if (!membership.getAirline().getId().equals(airlineId)) {
            throw new ResourceNotFoundException(String.format(ErrorMessageUtil.MEMBERSHIP_NOT_FOUND, membershipId, airlineId));
        }
        return membership;
    }

    private void requireInvitableRole(Long roleId) {
        if (!adminRoleId.equals(roleId) && !viewerRoleId.equals(roleId)) {
            throw new BadRequestException(String.format(ErrorMessageUtil.MEMBER_INVITE_TARGET_ROLE_INVALID, roleId));
        }
    }

    private MembershipStatus parseActivatableStatus(String status) {
        MembershipStatus parsed;
        try {
            parsed = MembershipStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorMessageUtil.MEMBER_STATUS_UPDATE_INVALID);
        }
        if (parsed != MembershipStatus.ACTIVE && parsed != MembershipStatus.SUSPENDED) {
            throw new BadRequestException(ErrorMessageUtil.MEMBER_STATUS_UPDATE_INVALID);
        }
        return parsed;
    }

    private void requireNotLastActiveOwner(AirlineMembership membership) {
        if (!ownerRoleId.equals(membership.getRoleId()) || membership.getStatus() != MembershipStatus.ACTIVE) {
            return;
        }
        long activeOwners = membershipRepository.countByAirlineIdAndRoleIdAndStatus(
                membership.getAirline().getId(), ownerRoleId, MembershipStatus.ACTIVE);
        if (activeOwners <= 1) {
            throw new ConflictException(String.format(ErrorMessageUtil.MEMBER_CANNOT_REMOVE_LAST_OWNER, membership.getAirline().getId()));
        }
    }

    /** Fails closed (503) when user-service cannot answer; 400 when the email matches no account. */
    private Long resolveUserByEmail(String email) {
        try {
            ReviewerResponse user = userClient.getUserByEmail(email);
            return user.getUserId();
        } catch (FeignException.NotFound e) {
            throw new BadRequestException(String.format(ErrorMessageUtil.MEMBER_INVITE_USER_NOT_FOUND, email));
        } catch (Exception e) {
            throw new ServiceUnavailableException(ErrorMessageUtil.USER_ROLE_CHECK_UNAVAILABLE, e);
        }
    }
}
