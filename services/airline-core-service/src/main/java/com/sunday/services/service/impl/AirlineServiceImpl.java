package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.AirlineRequest;
import com.sunday.common_lib.payload.response.AirlineDropdownItem;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.enums.MembershipStatus;
import com.sunday.services.mapper.AirlineMapper;
import com.sunday.services.model.Airline;
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

@Service
@RequiredArgsConstructor
@Transactional
public class AirlineServiceImpl implements AirlineService {

    private final AirlineRepository airlineRepository;
    private final AirlineMembershipRepository airlineMembershipRepository;

    // ---------- CRUD ----------

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "airlinesByUser", key = "#userId")
    public List<AirlineResponse> getMyAirlines(Long userId) {
        return airlineMembershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE).stream()
                .map(membership -> AirlineMapper.toResponse(membership.getAirline()))
                .toList();
    }

    @Override
    @Cacheable(cacheNames = "airlines", key = "#id")
    public AirlineResponse getAirlineById(Long id) {
        return AirlineMapper.toResponse(getAirlineOrThrow(id));
    }

    @Override
    public Page<AirlineResponse> getAllAirlines(Pageable pageable) {
        return airlineRepository
                .findAll(pageable).map(AirlineMapper::toResponse);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "airlines", key = "#airlineId"),
            @CacheEvict(cacheNames = "airlinesByUser", allEntries = true),
            @CacheEvict(cacheNames = "airlinesDropdown", allEntries = true)
    })
    public AirlineResponse updateAirline(Long airlineId, AirlineRequest request, Long userId) {
        Airline airline = getAirlineOrThrow(airlineId);
        requireActiveMembership(airlineId, userId);
        requireActiveAirline(airline);
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
            @CacheEvict(cacheNames = "airlines", key = "#id"),
            @CacheEvict(cacheNames = "airlinesByUser", allEntries = true),
            @CacheEvict(cacheNames = "airlinesDropdown", allEntries = true)
    })
    public void deleteAirline(Long id, Long userId) {
        Airline airline = getAirlineOrThrow(id);
        requireActiveMembership(id, userId);
        requireActiveAirline(airline);
        airline.setStatus(AirlineStatus.INACTIVE);
        airlineRepository.save(airline);
    }

    // ---------- Business Operations ----------

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = "airlines", key = "#airlineId"),
            @CacheEvict(cacheNames = "airlinesByUser", allEntries = true),
            @CacheEvict(cacheNames = "airlinesDropdown", allEntries = true)
    })
    public AirlineResponse changeStatusByAdmin(Long airlineId, AirlineStatus status) {
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
    @Cacheable(cacheNames = "airlinesDropdown")
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

    private Airline getAirlineOrThrow(Long id) {
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
    private void requireUniqueCodes(Long airlineId, AirlineRequest request) {
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

    private void requireActiveMembership(Long airlineId, Long userId) {
        if (!airlineMembershipRepository.existsByUserIdAndAirlineIdAndStatus(userId, airlineId, MembershipStatus.ACTIVE)) {
            throw new OperationNotPermittedException(String.format(ErrorMessageUtil.NO_ACTIVE_MEMBERSHIP_FOR_AIRLINE, airlineId));
        }
    }
}
