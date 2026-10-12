package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.constants.AncillaryPermissions;
import com.sunday.common_lib.enums.AncillaryType;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.FlightCabinAncillaryRequest;
import com.sunday.common_lib.payload.response.FlightCabinAncillaryBulkCreateResponse;
import com.sunday.common_lib.payload.response.FlightCabinAncillaryResponse;
import com.sunday.common_lib.payload.response.InsuranceCoverageResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.mapper.FlightCabinAncillaryMapper;
import com.sunday.services.mapper.InsuranceCoverageMapper;
import com.sunday.services.model.Ancillary;
import com.sunday.services.model.FlightCabinAncillary;
import com.sunday.services.model.InsuranceCoverage;
import com.sunday.services.repository.AncillaryRepository;
import com.sunday.services.repository.FlightCabinAncillaryRepository;
import com.sunday.services.repository.InsuranceCoverageRepository;
import com.sunday.services.service.FlightCabinAncillaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightCabinAncillaryServiceImpl implements FlightCabinAncillaryService {

    public static final int MAX_BULK_ROWS = 500;

    private final FlightCabinAncillaryRepository repository;
    private final AncillaryRepository ancillaryRepository;
    private final InsuranceCoverageRepository insuranceCoverageRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    private FlightCabinAncillaryResponse mapWithCoverages(FlightCabinAncillary entity) {
        List<InsuranceCoverage> coverages = insuranceCoverageRepository.findByAncillary(entity.getAncillary());
        List<InsuranceCoverageResponse> coverageResponses = coverages.stream()
                .map(InsuranceCoverageMapper::toResponse)
                .toList();
        return FlightCabinAncillaryMapper.toResponse(entity, coverageResponses);
    }

    /** Batch variant of {@link #mapWithCoverages(FlightCabinAncillary)}: one coverage query for the whole list. */
    private List<FlightCabinAncillaryResponse> mapWithCoverages(List<FlightCabinAncillary> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<UUID, Ancillary> ancillaryById = entities.stream()
                .collect(Collectors.toMap(e -> e.getAncillary().getId(), FlightCabinAncillary::getAncillary, (a, b) -> a));
        Map<UUID, List<InsuranceCoverageResponse>> coveragesByAncillaryId = insuranceCoverageRepository
                .findByAncillaryIn(ancillaryById.values()).stream()
                .collect(Collectors.groupingBy(
                        c -> c.getAncillary().getId(),
                        Collectors.mapping(InsuranceCoverageMapper::toResponse, Collectors.toList())));
        return entities.stream()
                .map(e -> FlightCabinAncillaryMapper.toResponse(
                        e, coveragesByAncillaryId.getOrDefault(e.getAncillary().getId(), List.of())))
                .collect(Collectors.toList());
    }

    private static String duplicateKey(UUID flightId, UUID cabinClassId, UUID ancillaryId) {
        return flightId + ":" + cabinClassId + ":" + ancillaryId;
    }

    @Override
    public FlightCabinAncillaryResponse create(UUID userId, FlightCabinAncillaryRequest req) {
        Ancillary ancillary = ancillaryRepository.findById(req.getAncillaryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.ANCILLARY_NOT_FOUND_BY_ID, req.getAncillaryId())));
        airlineIntegrationService.requirePermission(userId, ancillary.getAirlineId(), AncillaryPermissions.MANAGE);

        if (repository.existsByFlightIdAndCabinClassIdAndAncillary_Id(req.getFlightId(), req.getCabinClassId(), req.getAncillaryId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_ALREADY_ASSIGNED,
                    req.getAncillaryId(), req.getFlightId(), req.getCabinClassId()));
        }

        FlightCabinAncillary entity = FlightCabinAncillary.builder()
                .flightId(req.getFlightId())
                .cabinClassId(req.getCabinClassId())
                .ancillary(ancillary)
                .available(req.getAvailable())
                .maxQuantity(req.getMaxQuantity())
                .price(req.getPrice())
                .currency(req.getCurrency())
                .includedInFare(req.getIncludedInFare())
                .build();

        return mapWithCoverages(repository.save(entity));
    }

    @Override
    @Transactional
    public FlightCabinAncillaryBulkCreateResponse bulkCreate(UUID userId, List<FlightCabinAncillaryRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return FlightCabinAncillaryBulkCreateResponse.builder()
                    .created(List.of())
                    .skipped(List.of())
                    .build();
        }
        if (requests.size() > MAX_BULK_ROWS) {
            throw new BadRequestException(String.format(ErrorMessageUtil.BULK_TOO_LARGE, MAX_BULK_ROWS));
        }

        List<UUID> ancillaryIds = requests.stream()
                .map(FlightCabinAncillaryRequest::getAncillaryId)
                .distinct()
                .toList();
        Map<UUID, Ancillary> ancillaryById = ancillaryRepository.findAllById(ancillaryIds).stream()
                .collect(Collectors.toMap(Ancillary::getId, a -> a));

        Set<UUID> airlineIds = ancillaryById.values().stream()
                .map(Ancillary::getAirlineId)
                .collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, AncillaryPermissions.MANAGE);

        List<UUID> flightIds = requests.stream().map(FlightCabinAncillaryRequest::getFlightId).distinct().toList();
        List<UUID> cabinClassIds = requests.stream().map(FlightCabinAncillaryRequest::getCabinClassId).distinct().toList();
        Set<String> existingKeys = repository
                .findByFlightIdInAndCabinClassIdInAndAncillary_IdIn(flightIds, cabinClassIds, ancillaryIds).stream()
                .map(e -> duplicateKey(e.getFlightId(), e.getCabinClassId(), e.getAncillary().getId()))
                .collect(Collectors.toSet());

        List<FlightCabinAncillaryBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();
        List<FlightCabinAncillary> toInsert = new ArrayList<>();

        for (FlightCabinAncillaryRequest req : requests) {
            Ancillary ancillary = ancillaryById.get(req.getAncillaryId());
            if (ancillary == null) {
                skipped.add(FlightCabinAncillaryBulkCreateResponse.SkippedRequest.builder()
                        .request(req)
                        .reason(String.format(ErrorMessageUtil.ANCILLARY_NOT_FOUND_BY_ID, req.getAncillaryId()))
                        .build());
                continue;
            }
            String key = duplicateKey(req.getFlightId(), req.getCabinClassId(), req.getAncillaryId());
            if (existingKeys.contains(key)) {
                skipped.add(FlightCabinAncillaryBulkCreateResponse.SkippedRequest.builder()
                        .request(req)
                        .reason(String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_ALREADY_ASSIGNED,
                                req.getAncillaryId(), req.getFlightId(), req.getCabinClassId()))
                        .build());
                continue;
            }
            if (!seenInBatch.add(key)) {
                skipped.add(FlightCabinAncillaryBulkCreateResponse.SkippedRequest.builder()
                        .request(req)
                        .reason(String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_DUPLICATE_IN_REQUEST,
                                req.getAncillaryId(), req.getFlightId(), req.getCabinClassId()))
                        .build());
                continue;
            }
            toInsert.add(FlightCabinAncillary.builder()
                    .flightId(req.getFlightId())
                    .cabinClassId(req.getCabinClassId())
                    .ancillary(ancillary)
                    .available(req.getAvailable())
                    .maxQuantity(req.getMaxQuantity())
                    .price(req.getPrice())
                    .currency(req.getCurrency())
                    .includedInFare(req.getIncludedInFare())
                    .build());
        }

        List<FlightCabinAncillaryResponse> created = mapWithCoverages(repository.saveAll(toInsert));

        return FlightCabinAncillaryBulkCreateResponse.builder()
                .created(created)
                .skipped(skipped)
                .build();
    }

    @Override
    public FlightCabinAncillaryResponse getById(UUID id) {
        FlightCabinAncillary entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_NOT_FOUND_BY_ID, id)));
        return mapWithCoverages(entity);
    }

    @Override
    public List<FlightCabinAncillaryResponse> getAllByFlightAndCabinClass(UUID flightId, UUID cabinClassId) {
        return mapWithCoverages(repository.findByFlightIdAndCabinClassId(flightId, cabinClassId));
    }

    @Override
    public List<FlightCabinAncillaryResponse> getAllByIds(List<UUID> ids) {
        return mapWithCoverages(repository.findAllById(ids));
    }

    @Override
    public FlightCabinAncillaryResponse getByFlightIdAndCabinClassAndType(
            UUID flightId, UUID cabinClassId, AncillaryType type) {
        FlightCabinAncillary entity = repository
                .findByFlightIdAndCabinClassIdAndAncillary_Type(flightId, cabinClassId, type)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_NOT_FOUND_FOR_TYPE, type)));
        return mapWithCoverages(entity);
    }

    @Override
    public List<FlightCabinAncillaryResponse> getAllByFlightIdAndCabinClassAndType(
            UUID flightId, UUID cabinClassId, AncillaryType type) {
        return mapWithCoverages(repository.findAllByFlightIdAndCabinClassIdAndAncillary_Type(
                flightId, cabinClassId, type));
    }

    @Override
    public FlightCabinAncillaryResponse update(UUID userId, UUID id, FlightCabinAncillaryRequest req) {
        FlightCabinAncillary entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, entity.getAncillary().getAirlineId(), AncillaryPermissions.MANAGE);

        entity.setAvailable(req.getAvailable());
        entity.setMaxQuantity(req.getMaxQuantity());
        entity.setPrice(req.getPrice());
        entity.setCurrency(req.getCurrency());
        entity.setIncludedInFare(req.getIncludedInFare());

        return mapWithCoverages(repository.save(entity));
    }

    @Override
    public void delete(UUID userId, UUID id) {
        FlightCabinAncillary entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, entity.getAncillary().getAirlineId(), AncillaryPermissions.MANAGE);
        repository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateAncillaryPrice(UUID flightId, List<UUID> ancillaryIds) {
        if (ancillaryIds == null || ancillaryIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<FlightCabinAncillary> ancillaries = repository.findAllById(ancillaryIds);

        // findAllById silently skips unknown IDs — reject them instead of undercharging
        Set<UUID> foundIds = ancillaries.stream()
                .map(FlightCabinAncillary::getId)
                .collect(Collectors.toSet());
        List<UUID> missingIds = ancillaryIds.stream()
                .distinct()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_NOT_FOUND_FOR_IDS, missingIds));
        }

        List<String> wrongFlight = ancillaries.stream()
                .filter(item -> !item.getFlightId().equals(flightId))
                .map(item -> String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_WRONG_FLIGHT, item.getId(), flightId))
                .toList();
        if (!wrongFlight.isEmpty()) {
            throw new ConflictException(String.join("; ", wrongFlight));
        }

        Map<UUID, Long> quantities = ancillaryIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        BigDecimal totalPrice = BigDecimal.ZERO;
        for (FlightCabinAncillary item : ancillaries) {
            String label = item.getAncillary().getName() + " (id " + item.getId() + ")";
            long quantity = quantities.get(item.getId());
            if (!Boolean.TRUE.equals(item.getAvailable())) {
                throw new BadRequestException(String.format(ErrorMessageUtil.ANCILLARY_NOT_AVAILABLE, label));
            }
            if (item.getMaxQuantity() != null && quantity > item.getMaxQuantity()) {
                throw new BadRequestException(String.format(
                        ErrorMessageUtil.ANCILLARY_MAX_QUANTITY_EXCEEDED, label, item.getMaxQuantity(), quantity));
            }
            if (Boolean.TRUE.equals(item.getIncludedInFare())) {
                continue;
            }
            if (item.getPrice() == null) {
                throw new BadRequestException(String.format(ErrorMessageUtil.ANCILLARY_PRICE_NOT_SET, label));
            }
            totalPrice = totalPrice.add(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        return totalPrice;
    }
}
