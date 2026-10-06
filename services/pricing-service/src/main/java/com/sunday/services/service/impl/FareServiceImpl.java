package com.sunday.services.service.impl;


import com.sunday.common_lib.constants.PricingPermissions;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.FareRequest;
import com.sunday.common_lib.payload.response.FareBulkCreateResponse;
import com.sunday.common_lib.payload.response.FareBulkCreateResponse.SkippedRequest;
import com.sunday.common_lib.payload.response.FareResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.integration.PricingIntegrationService;
import com.sunday.services.mapper.FareMapper;
import com.sunday.services.model.Fare;
import com.sunday.services.repository.FareRepository;
import com.sunday.services.service.FareService;
import com.sunday.services.validation.FareRequestValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import com.sunday.services.config.FareCacheEvictor;
import com.sunday.services.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FareServiceImpl implements FareService {

    public static final int MAX_BULK_ROWS = 500;

    private final FareRepository fareRepository;
    private final PricingIntegrationService pricingIntegrationService;
    private final Validator validator;

    @Override
    public FareResponse createFare(Long userId, FareRequest request) {
        Long airlineId = pricingIntegrationService
                .resolveAirlineIdsByFlightIds(List.of(request.getFlightId()))
                .get(request.getFlightId());
        pricingIntegrationService.requirePermission(userId, airlineId, PricingPermissions.FARE_MANAGE);

        FareRequestValidator.validate(request);
        if (fareRepository.existsByAirlineIdAndFlightIdAndCabinClassAndRbdCodeAndPassengerTypeAndName(
                airlineId, request.getFlightId(), request.getCabinClass(), request.getRbdCode(),
                FareMapper.passengerTypeOf(request), request.getName())) {
            throw new ConflictException(String.format(ErrorMessageUtil.FARE_ALREADY_EXISTS, request.getName()));
        }
        Fare saved = fareRepository.save(FareMapper.toEntity(request, airlineId, userId));
        return FareMapper.toResponse(saved);
    }

    /**
     * Partial success: each row is checked in memory (bean validation, then the window and currency rules). Rows
     * that pass are checked against the flight lookup, the stored fares and earlier rows in the same request. Each
     * row that fails any check is reported in {@code skipped}; the rest are saved in one batch. The flight lookup,
     * the permission check and the stored-fare lookup each run once for the whole request.
     */
    @Override
    public FareBulkCreateResponse createFares(Long userId, List<FareRequest> requests) {
        if (requests.size() > MAX_BULK_ROWS) {
            throw new BadRequestException(String.format(ErrorMessageUtil.BULK_TOO_LARGE, MAX_BULK_ROWS));
        }

        List<SkippedRequest> skipped = new ArrayList<>();
        List<IndexedRequest> candidates = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            FareRequest request = requests.get(index);
            Optional<String> problem = problemWith(request);
            if (problem.isPresent()) {
                skipped.add(skip(index, request, problem.get()));
            } else {
                candidates.add(new IndexedRequest(index, request));
            }
        }

        Set<Long> requestedFlightIds = candidates.stream()
                .map(candidate -> candidate.request().getFlightId())
                .collect(Collectors.toSet());
        Map<Long, Long> airlineByFlight = pricingIntegrationService.findAirlineIdsByFlightIds(requestedFlightIds);

        List<IndexedRequest> known = new ArrayList<>();
        for (IndexedRequest candidate : candidates) {
            Long flightId = candidate.request().getFlightId();
            if (airlineByFlight.containsKey(flightId)) {
                known.add(candidate);
            } else {
                skipped.add(skip(candidate.index(), candidate.request(),
                        String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, flightId)));
            }
        }

        List<FareResponse> created = List.of();
        if (!known.isEmpty()) {
            Set<Long> knownFlightIds = known.stream().map(c -> c.request().getFlightId()).collect(Collectors.toSet());
            Set<Long> knownAirlineIds = knownFlightIds.stream().map(airlineByFlight::get).collect(Collectors.toSet());
            pricingIntegrationService.requirePermission(userId, knownAirlineIds, PricingPermissions.FARE_MANAGE);

            Set<String> stored = fareRepository.findByFlightIdIn(knownFlightIds).stream()
                    .map(Fare::naturalKey)
                    .collect(Collectors.toCollection(HashSet::new));

            Set<String> seenInRequest = new HashSet<>();
            List<Fare> toSave = new ArrayList<>();
            for (IndexedRequest candidate : known) {
                FareRequest request = candidate.request();
                Long airlineId = airlineByFlight.get(request.getFlightId());
                String key = Fare.keyOf(airlineId, request.getFlightId(), request.getCabinClass(), request.getRbdCode(),
                        FareMapper.passengerTypeOf(request), request.getName());
                if (stored.contains(key)) {
                    skipped.add(skip(candidate.index(), request, String.format(ErrorMessageUtil.FARE_ALREADY_EXISTS, request.getName())));
                } else if (!seenInRequest.add(key)) {
                    skipped.add(skip(candidate.index(), request, ErrorMessageUtil.FARE_DUPLICATE_IN_REQUEST));
                } else {
                    toSave.add(FareMapper.toEntity(request, airlineId, userId));
                }
            }
            created = fareRepository.saveAll(toSave).stream()
                    .map(FareMapper::toResponse)
                    .collect(Collectors.toList());
        }

        skipped.sort(Comparator.comparingInt(SkippedRequest::getIndex));
        return FareBulkCreateResponse.builder().created(created).skipped(skipped).build();
    }

    private record IndexedRequest(int index, FareRequest request) {}

    private Optional<String> problemWith(FareRequest request) {
        Set<ConstraintViolation<FareRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            return Optional.of(violations.iterator().next().getMessage());
        }
        try {
            FareRequestValidator.validate(request);
            return Optional.empty();
        } catch (BadRequestException e) {
            return Optional.of(e.getMessage());
        }
    }

    private static SkippedRequest skip(int index, FareRequest request, String reason) {
        return SkippedRequest.builder().index(index).request(request).reason(reason).build();
    }

    @Override
    @Transactional(readOnly = true)
    public FareResponse getFareById(Long userId, Long id) {
        Fare fare = findFareOrThrow(id);
        pricingIntegrationService.requirePermission(userId, fare.getAirlineId(), PricingPermissions.FARE_READ);
        return FareMapper.toResponse(fare);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FareResponse> getFaresByFlightIdAndCabinClassId(Long userId, Long flightId, Long cabinClassId) {
        List<Fare> fares = fareRepository.findByFlightIdAndCabinClassId(flightId, cabinClassId);
        if (!fares.isEmpty()) {
            Set<Long> airlineIds = fares.stream().map(Fare::getAirlineId).collect(Collectors.toSet());
            pricingIntegrationService.requirePermission(userId, airlineIds, PricingPermissions.FARE_READ);
        }
        return fares.stream().map(FareMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FareResponse> getFaresByAirline(Long userId, Long airlineId, Pageable pageable) {
        pricingIntegrationService.requirePermission(userId, airlineId, PricingPermissions.FARE_READ);
        return fareRepository.findByAirlineId(airlineId, pageable).map(FareMapper::toResponse);
    }

    @Override
    @CacheEvict(cacheNames = RedisConfig.FARES, key = "#id")
    public FareResponse updateFare(Long userId, Long id, FareRequest request) {
        Fare existing = findFareOrThrow(id);
        pricingIntegrationService.requirePermission(userId, existing.getAirlineId(), PricingPermissions.FARE_MANAGE);

        FareRequestValidator.validate(request);
        if (fareRepository.existsByAirlineIdAndFlightIdAndCabinClassAndRbdCodeAndPassengerTypeAndNameAndIdNot(
                existing.getAirlineId(), existing.getFlightId(), request.getCabinClass(), request.getRbdCode(),
                FareMapper.passengerTypeOf(request), request.getName(), id)) {
            throw new ConflictException(String.format(ErrorMessageUtil.FARE_ALREADY_EXISTS, request.getName()));
        }

        FareMapper.updateEntity(request, existing, userId);
        return FareMapper.toResponse(fareRepository.save(existing));
    }

    @Override
    @CacheEvict(cacheNames = RedisConfig.FARES, key = "#id")
    public void deleteFare(Long userId, Long id) {
        Fare fare = findFareOrThrow(id);
        pricingIntegrationService.requirePermission(userId, fare.getAirlineId(), PricingPermissions.FARE_MANAGE);
        fareRepository.delete(fare);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisConfig.FARES, key = "#id")
    public FareResponse getFareByIdInternal(Long id) {
        return FareMapper.toResponse(findFareOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, FareResponse> getFaresByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return fareRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Fare::getId, FareMapper::toResponse));
    }

    private Fare findFareOrThrow(Long id) {
        return fareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.FARE_NOT_FOUND_BY_ID, id)));
    }
}
