package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.constants.AircraftPermissions;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.AircraftRequest;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.mapper.AircraftMapper;
import com.sunday.services.model.Aircraft;
import com.sunday.services.model.Airline;
import com.sunday.services.repository.AircraftRepository;
import com.sunday.services.repository.AirlineRepository;
import com.sunday.services.service.AircraftService;
import com.sunday.services.service.AirlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AircraftServiceImpl implements AircraftService {

    private final AircraftRepository aircraftRepository;
    private final AirlineRepository airlineRepository;
    private final AirlineService airlineService;

    @Override
    public AircraftResponse createAircraft(AircraftRequest request, UUID userId) {
        UUID airlineId = request.getAirlineId();
        if (airlineId == null) {
            throw new BadRequestException(ErrorMessageUtil.AIRLINE_ID_REQUIRED);
        }
        airlineService.requirePermission(userId, List.of(airlineId), AircraftPermissions.MANAGE);
        Airline airline = getAirlineOrThrow(airlineId);

        Aircraft aircraft = AircraftMapper.toEntity(request, airline);
        if (aircraftRepository.existsByCode(aircraft.getCode())) {
            throw new ConflictException(String.format(ErrorMessageUtil.AIRCRAFT_CODE_ALREADY_EXISTS, aircraft.getCode()));
        }

        validateAircraftData(aircraft);
        return AircraftMapper.toResponse(aircraftRepository.save(aircraft));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "aircrafts", key = "#id")
    public AircraftResponse getAircraftById(UUID id, UUID userId) {
        Aircraft aircraft = getAircraftOrThrow(id);
        airlineService.requirePermission(userId, List.of(aircraft.getAirline().getId()), AircraftPermissions.READ);
        return AircraftMapper.toResponse(aircraft);
    }

    @Override
    @Transactional(readOnly = true)
    public AircraftResponse getAircraftByIdInternal(UUID id) {
        return AircraftMapper.toResponse(getAircraftOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, AircraftResponse> getAircraftsByIdsInternal(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return aircraftRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Aircraft::getId, AircraftMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AircraftResponse> listAircraftsForAirline(UUID airlineId, UUID userId) {
        airlineService.requirePermission(userId, List.of(airlineId), AircraftPermissions.READ);
        Airline airline = getAirlineOrThrow(airlineId);
        return aircraftRepository.findByAirline(airline)
                .stream()
                .map(AircraftMapper::toResponse)
                .toList();
    }

    @Override
    @CacheEvict(cacheNames = "aircrafts", key = "#id")
    public AircraftResponse updateAircraft(UUID id, AircraftRequest request, UUID userId) {
        Aircraft aircraft = getAircraftOrThrow(id);
        airlineService.requirePermission(userId, List.of(aircraft.getAirline().getId()), AircraftPermissions.MANAGE);

        String oldCode = aircraft.getCode();
        if (!oldCode.equals(request.getCode()) && aircraftRepository.existsByCode(request.getCode())) {
            throw new ConflictException(String.format(ErrorMessageUtil.AIRCRAFT_CODE_ALREADY_EXISTS, request.getCode()));
        }

        AircraftMapper.updateEntity(aircraft, request);
        validateAircraftData(aircraft);
        return AircraftMapper.toResponse(aircraftRepository.save(aircraft));
    }

    @Override
    @CacheEvict(cacheNames = "aircrafts", key = "#id")
    public void deleteAircraft(UUID id, UUID userId) {
        Aircraft aircraft = getAircraftOrThrow(id);
        airlineService.requirePermission(userId, List.of(aircraft.getAirline().getId()), AircraftPermissions.MANAGE);
        aircraftRepository.delete(aircraft);
    }

    // ---------- Helpers ----------

    private Aircraft getAircraftOrThrow(UUID id) {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRCRAFT_NOT_FOUND_BY_ID, id)));
    }

    private Airline getAirlineOrThrow(UUID airlineId) {
        return airlineRepository.findById(airlineId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRLINE_NOT_FOUND_BY_ID, airlineId)));
    }

    private void validateAircraftData(Aircraft aircraft) {
        int totalSpecifiedSeats = (aircraft.getEconomySeats() != null ? aircraft.getEconomySeats() : 0) +
                (aircraft.getPremiumEconomySeats() != null ? aircraft.getPremiumEconomySeats() : 0) +
                (aircraft.getBusinessSeats() != null ? aircraft.getBusinessSeats() : 0) +
                (aircraft.getFirstClassSeats() != null ? aircraft.getFirstClassSeats() : 0);

        if (totalSpecifiedSeats > aircraft.getSeatingCapacity()) {
            throw new BadRequestException(ErrorMessageUtil.AIRCRAFT_SEATS_EXCEED_CAPACITY);
        }

        if (aircraft.getYearOfManufacture() != null &&
                (aircraft.getYearOfManufacture() < 1900 || aircraft.getYearOfManufacture() > LocalDate.now().getYear())) {
            throw new BadRequestException(ErrorMessageUtil.AIRCRAFT_INVALID_YEAR_OF_MANUFACTURE);
        }
    }
}
