package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.AncillaryPermissions;
import com.sunday.common_lib.enums.AncillaryType;
import com.sunday.common_lib.exception.BadRequestException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlightCabinAncillaryServiceImpl implements FlightCabinAncillaryService {

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

    @Override
    public FlightCabinAncillaryResponse create(Long userId, FlightCabinAncillaryRequest req) {
        Ancillary ancillary = ancillaryRepository.findById(req.getAncillaryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.ANCILLARY_NOT_FOUND_BY_ID, req.getAncillaryId())));
        airlineIntegrationService.requirePermission(userId, ancillary.getAirlineId(), AncillaryPermissions.MANAGE);

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
    public FlightCabinAncillaryBulkCreateResponse bulkCreate(Long userId, List<FlightCabinAncillaryRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return FlightCabinAncillaryBulkCreateResponse.builder()
                    .created(List.of())
                    .skipped(List.of())
                    .build();
        }

        List<Long> ancillaryIds = requests.stream()
                .map(FlightCabinAncillaryRequest::getAncillaryId)
                .distinct()
                .toList();
        Map<Long, Ancillary> ancillaryById = ancillaryRepository.findAllById(ancillaryIds).stream()
                .collect(Collectors.toMap(Ancillary::getId, a -> a));

        Set<Long> airlineIds = ancillaryById.values().stream()
                .map(Ancillary::getAirlineId)
                .collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, AncillaryPermissions.MANAGE);

        List<FlightCabinAncillaryBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();
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

        List<FlightCabinAncillaryResponse> created = repository.saveAll(toInsert).stream()
                .map(this::mapWithCoverages)
                .collect(Collectors.toList());

        return FlightCabinAncillaryBulkCreateResponse.builder()
                .created(created)
                .skipped(skipped)
                .build();
    }

    @Override
    public FlightCabinAncillaryResponse getById(Long id) {
        FlightCabinAncillary entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_NOT_FOUND_BY_ID, id)));
        return mapWithCoverages(entity);
    }

    @Override
    public List<FlightCabinAncillaryResponse> getAllByFlightAndCabinClass(Long flightId, Long cabinClassId) {
        return repository.findByFlightIdAndCabinClassId(flightId, cabinClassId).stream()
                .map(this::mapWithCoverages)
                .collect(Collectors.toList());
    }

    @Override
    public List<FlightCabinAncillaryResponse> getAllByIds(List<Long> ids) {
        List<FlightCabinAncillary> ancillaries = repository.findAllById(ids);
        return ancillaries.stream().map(this::mapWithCoverages).collect(Collectors.toList());
    }

    @Override
    public FlightCabinAncillaryResponse getByFlightIdAndCabinClassAndType(
            Long flightId, Long cabinClassId, AncillaryType type) {
        FlightCabinAncillary entity = repository
                .findByFlightIdAndCabinClassIdAndAncillary_Type(flightId, cabinClassId, type)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_NOT_FOUND_FOR_TYPE, type)));
        return mapWithCoverages(entity);
    }

    @Override
    public List<FlightCabinAncillaryResponse> getAllByFlightIdAndCabinClassAndType(
            Long flightId, Long cabinClassId, AncillaryType type) {
        List<FlightCabinAncillary> ancillaries = repository.findAllByFlightIdAndCabinClassIdAndAncillary_Type(
                flightId, cabinClassId, type);
        return ancillaries.stream().map(this::mapWithCoverages).collect(Collectors.toList());
    }

    @Override
    public FlightCabinAncillaryResponse update(Long userId, Long id, FlightCabinAncillaryRequest req) {
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
    public void delete(Long userId, Long id) {
        FlightCabinAncillary entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, entity.getAncillary().getAirlineId(), AncillaryPermissions.MANAGE);
        repository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateAncillaryPrice(List<Long> ancillaryIds) {
        if (ancillaryIds == null || ancillaryIds.isEmpty()) {
            return 0.0;
        }

        List<FlightCabinAncillary> ancillaries = repository.findAllById(ancillaryIds);

        // findAllById silently skips unknown IDs — reject them instead of undercharging
        Set<Long> foundIds = ancillaries.stream()
                .map(FlightCabinAncillary::getId)
                .collect(Collectors.toSet());
        List<Long> missingIds = ancillaryIds.stream()
                .distinct()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(ErrorMessageUtil.FLIGHT_CABIN_ANCILLARY_NOT_FOUND_FOR_IDS, missingIds));
        }

        Map<Long, Long> quantities = ancillaryIds.stream()
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

        double totalPrice = 0;
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
            totalPrice += item.getPrice() * quantity;
        }
        return totalPrice;
    }
}
