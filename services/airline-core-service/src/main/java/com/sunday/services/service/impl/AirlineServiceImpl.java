package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.constants.AirlinePermissions;
import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.AirlineRequest;
import com.sunday.common_lib.payload.response.AirlineDropdownItem;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.common_lib.util.CacheUtil;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.mapper.AirlineMapper;
import com.sunday.services.model.Airline;
import com.sunday.services.model.AirlineMembership;
import com.sunday.services.repository.AirlineMembershipRepository;
import com.sunday.services.repository.AirlineRepository;
import com.sunday.services.service.AirlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AirlineServiceImpl implements AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirlineMembershipRepository airlineMembershipRepository;
    private final RolePermissionCacheService rolePermissionCacheService;

    // ---------- CRUD ----------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheUtil.AIRLINES_BY_USER, key = "#userId")
    public List<AirlineResponse> getMyAirlines(UUID userId) {
        return airlineMembershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE).stream()
                .map(membership -> AirlineMapper.toResponse(membership.getAirline()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getMyPermissions(UUID airlineId, UUID userId) {
        UUID roleId = activeRoleIdsByAirline(userId, List.of(airlineId)).get(airlineId);
        if (roleId == null) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.NO_ACTIVE_MEMBERSHIP_IN_ACTIVE_AIRLINE, airlineId));
        }
        return rolePermissionCacheService.getPermissionNamesForRole(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requirePermission(UUID userId, List<UUID> airlineIds, String permission) {
        Map<UUID, UUID> roleIds = activeRoleIdsByAirline(userId, airlineIds);
        List<UUID> denied = airlineIds.stream()
                .distinct()
                .filter(airlineId -> !roleIds.containsKey(airlineId)
                        || !rolePermissionCacheService.getPermissionNamesForRole(roleIds.get(airlineId)).contains(permission))
                .toList();
        if (!denied.isEmpty()) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRLINES, permission, denied));
        }
    }

    /** A suspended, banned or closed airline grants nothing: its members keep the membership but lose every permission. */
    private Map<UUID, UUID> activeRoleIdsByAirline(UUID userId, List<UUID> airlineIds) {
        return airlineMembershipRepository.findByUserIdAndAirlineIdInAndStatusAndAirlineStatus(
                        userId, airlineIds, MembershipStatus.ACTIVE, AirlineStatus.ACTIVE).stream()
                .collect(Collectors.toMap(m -> m.getAirline().getId(), AirlineMembership::getRoleId));
    }

    // Deliberately not @Cacheable: the permission check must run on every call. Caching this method
    // would let the first caller's DB hit satisfy every later caller straight from cache, skipping
    // requireActiveMembership entirely and leaking the airline's data to non-members.
    @Override
    @Transactional(readOnly = true)
    public AirlineResponse getAirlineById(UUID id, UUID userId) {
        Airline airline = getAirlineOrThrow(id);
        requireActiveMembership(id, userId);
        return AirlineMapper.toResponse(airline);
    }

    @Override
    @Transactional(readOnly = true)
    public AirlineResponse getAirlineByIdInternal(UUID id) {
        return AirlineMapper.toResponse(getAirlineOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, AirlineResponse> getAirlinesByIdsInternal(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return airlineRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Airline::getId, AirlineMapper::toResponse));
    }

    @Override
    public Page<AirlineResponse> getAllAirlines(Pageable pageable) {
        return airlineRepository
                .findAll(pageable).map(AirlineMapper::toResponse);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheUtil.AIRLINES_BY_USER, allEntries = true),
            @CacheEvict(cacheNames = CacheUtil.AIRLINES_DROPDOWN, allEntries = true)
    })
    public AirlineResponse updateAirline(UUID airlineId, AirlineRequest request, UUID userId) {
        Airline airline = getAirlineOrThrow(airlineId);
        requireActiveAirline(airline);
        requirePermission(userId, List.of(airlineId), AirlinePermissions.PROFILE_UPDATE);
        requireUniqueCodes(airlineId, request);

        AirlineMapper.updateEntity(airline, request);
        return AirlineMapper.toResponse(airlineRepository.save(airline));
    }

    /**
     * Soft delete: the airline is marked INACTIVE rather than removed. A hard delete can never
     * succeed here — the caller's own membership row (and any aircraft) reference the airline by FK.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheUtil.AIRLINES_BY_USER, allEntries = true),
            @CacheEvict(cacheNames = CacheUtil.AIRLINES_DROPDOWN, allEntries = true)
    })
    public void deleteAirline(UUID id, UUID userId) {
        Airline airline = getAirlineOrThrow(id);
        requireActiveAirline(airline);
        requirePermission(userId, List.of(id), AirlinePermissions.CLOSE);
        airline.setStatus(AirlineStatus.INACTIVE);
        airlineRepository.save(airline);
    }

    // ---------- Business Operations ----------

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheUtil.AIRLINES_BY_USER, allEntries = true),
            @CacheEvict(cacheNames = CacheUtil.AIRLINES_DROPDOWN, allEntries = true)
    })
    public AirlineResponse changeStatusByAdmin(UUID airlineId, AirlineStatus status) {
        Airline airline = getAirlineOrThrow(airlineId);
        if (airline.getStatus() == AirlineStatus.INACTIVE) {
            throw new ConflictException(String.format(ErrorMessageUtil.AIRLINE_CLOSED_STATUS_LOCKED, airlineId));
        }
        if (airline.getStatus() == status) {
            throw new ConflictException(String.format(ErrorMessageUtil.AIRLINE_STATUS_UNCHANGED, airlineId, status));
        }
        airline.setStatus(status);
        return AirlineMapper.toResponse(airlineRepository.save(airline));
    }

    // ---------- Search / Filters ----------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheUtil.AIRLINES_DROPDOWN)
    public List<AirlineDropdownItem> getAirlinesForDropdown() {
        return airlineRepository.findByStatus(AirlineStatus.ACTIVE).stream()
                .map(a -> AirlineDropdownItem.builder()
                        .id(a.getId())
                        .name(a.getName())
                        .iataCode(a.getIataCode())
                        .icaoCode(a.getIcaoCode())
                        .logoUrl(a.getLogoUrl())
                        .country(a.getCountry())
                        .build())
                .toList();
    }

    // ---------- Helpers ----------

    private Airline getAirlineOrThrow(UUID id) {
        return airlineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRLINE_NOT_FOUND_BY_ID, id)));
    }

    private void requireActiveAirline(Airline airline) {
        if (airline.getStatus() != AirlineStatus.ACTIVE) {
            throw new OperationNotPermittedException(String.format(
                    ErrorMessageUtil.AIRLINE_NOT_ACTIVE, airline.getId(), airline.getStatus()));
        }
    }

    /** Pre-check for the DB unique constraints so a clash is a 409 instead of a 500. */
    private void requireUniqueCodes(UUID airlineId, AirlineRequest request) {
        if (request.getIataCode() != null) {
            airlineRepository.findByIataCode(request.getIataCode())
                    .filter(other -> !other.getId().equals(airlineId))
                    .ifPresent(other -> {
                        throw new ConflictException(String.format(
                                ErrorMessageUtil.AIRLINE_IATA_CODE_ALREADY_EXISTS, request.getIataCode()));
                    });
        }
        if (request.getIcaoCode() != null) {
            airlineRepository.findByIcaoCode(request.getIcaoCode())
                    .filter(other -> !other.getId().equals(airlineId))
                    .ifPresent(other -> {
                        throw new ConflictException(String.format(
                                ErrorMessageUtil.AIRLINE_ICAO_CODE_ALREADY_EXISTS, request.getIcaoCode()));
                    });
        }
    }

    private void requireActiveMembership(UUID airlineId, UUID userId) {
        if (!airlineMembershipRepository.existsByUserIdAndAirlineIdAndStatus(userId, airlineId, MembershipStatus.ACTIVE)) {
            throw new OperationNotPermittedException(String.format(ErrorMessageUtil.NO_ACTIVE_MEMBERSHIP_FOR_AIRLINE, airlineId));
        }
    }
}
