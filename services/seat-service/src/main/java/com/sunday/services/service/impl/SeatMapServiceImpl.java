package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.SeatPermissions;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.SeatMapRequest;
import com.sunday.common_lib.payload.response.SeatMapBulkCreateResponse;
import com.sunday.common_lib.payload.response.SeatMapResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.integration.AirlineIntegrationService;
import com.sunday.services.mapper.SeatMapMapper;
import com.sunday.services.model.CabinClass;
import com.sunday.services.model.SeatMap;
import com.sunday.services.repository.CabinClassRepository;
import com.sunday.services.repository.SeatMapRepository;
import com.sunday.services.service.SeatMapService;
import com.sunday.services.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatMapServiceImpl implements SeatMapService {

    private final SeatMapRepository seatMapRepository;
    private final CabinClassRepository cabinClassRepository;
    private final SeatService seatService;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    public SeatMapResponse createSeatMap(Long userId, SeatMapRequest request) {
        CabinClass cabinClass = cabinClassRepository.findById(request.getCabinClassId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.CABIN_CLASS_NOT_FOUND_BY_ID, request.getCabinClassId())));

        Long airlineId = airlineIntegrationService.resolveAirlineIdForAircraft(cabinClass.getAircraftId());
        airlineIntegrationService.requirePermission(userId, airlineId, SeatPermissions.SEAT_CONFIG_MANAGE);

        if (seatMapRepository.existsByAirlineIdAndCabinClassIdAndName(
                airlineId, request.getCabinClassId(), request.getName())) {
            throw new ConflictException(
                    String.format(ErrorMessageUtil.SEAT_MAP_NAME_ALREADY_EXISTS, request.getName()));
        }

        SeatMap seatMap = SeatMapMapper.toEntity(request, cabinClass);
        seatMap.setAirlineId(airlineId);
        SeatMap savedSeatMap = seatMapRepository.save(seatMap);

        seatService.generateSeats(List.of(savedSeatMap));
        return SeatMapMapper.toResponse(savedSeatMap);
    }

    @Override
    public SeatMapBulkCreateResponse createSeatMaps(Long userId, List<SeatMapRequest> requests) {
        if (requests.isEmpty()) {
            return SeatMapBulkCreateResponse.builder().created(List.of()).skipped(List.of()).build();
        }

        Set<Long> cabinClassIds = requests.stream()
                .map(SeatMapRequest::getCabinClassId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CabinClass> cabinClassesById = cabinClassRepository.findAllById(cabinClassIds).stream()
                .collect(Collectors.toMap(CabinClass::getId, Function.identity()));

        List<SeatMapBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();
        List<SeatMapRequest> withCabinClass = new ArrayList<>();
        for (SeatMapRequest req : requests) {
            if (cabinClassesById.containsKey(req.getCabinClassId())) {
                withCabinClass.add(req);
            } else {
                skipped.add(skip(req, String.format(ErrorMessageUtil.CABIN_CLASS_NOT_FOUND_BY_ID, req.getCabinClassId())));
            }
        }

        Set<Long> aircraftIds = withCabinClass.stream()
                .map(req -> cabinClassesById.get(req.getCabinClassId()).getAircraftId())
                .collect(Collectors.toSet());
        Map<Long, Long> airlineIdByAircraft = airlineIntegrationService.resolveAirlineIdsForAircrafts(aircraftIds);

        List<SeatMapRequest> resolvable = new ArrayList<>();
        for (SeatMapRequest req : withCabinClass) {
            Long aircraftId = cabinClassesById.get(req.getCabinClassId()).getAircraftId();
            if (airlineIdByAircraft.containsKey(aircraftId)) {
                resolvable.add(req);
            } else {
                skipped.add(skip(req, String.format(ErrorMessageUtil.AIRCRAFT_NOT_FOUND_BY_ID, aircraftId)));
            }
        }

        Set<Long> airlineIds = resolvable.stream()
                .map(req -> airlineIdByAircraft.get(cabinClassesById.get(req.getCabinClassId()).getAircraftId()))
                .collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, SeatPermissions.SEAT_CONFIG_MANAGE);

        Set<Long> resolvableCabinClassIds = resolvable.stream()
                .map(SeatMapRequest::getCabinClassId)
                .collect(Collectors.toSet());
        Set<Long> cabinClassesWithSeatMap = resolvableCabinClassIds.isEmpty()
                ? Set.of()
                : seatMapRepository.findByCabinClassIdIn(resolvableCabinClassIds).stream()
                        .map(sm -> sm.getCabinClass().getId())
                        .collect(Collectors.toSet());

        Set<Long> seenCabinClassIds = new HashSet<>();
        List<SeatMap> toSave = new ArrayList<>();
        for (SeatMapRequest req : resolvable) {
            CabinClass cabinClass = cabinClassesById.get(req.getCabinClassId());
            Long cabinClassId = cabinClass.getId();
            if (cabinClassesWithSeatMap.contains(cabinClassId)) {
                skipped.add(skip(req, String.format(ErrorMessageUtil.SEAT_MAP_CABIN_CLASS_ALREADY_HAS_SEAT_MAP, cabinClassId)));
            } else if (!seenCabinClassIds.add(cabinClassId)) {
                skipped.add(skip(req, String.format(ErrorMessageUtil.SEAT_MAP_CABIN_CLASS_DUPLICATE_IN_REQUEST, cabinClassId)));
            } else {
                SeatMap seatMap = SeatMapMapper.toEntity(req, cabinClass);
                seatMap.setAirlineId(airlineIdByAircraft.get(cabinClass.getAircraftId()));
                toSave.add(seatMap);
            }
        }

        List<SeatMap> saved = seatMapRepository.saveAll(toSave);
        seatService.generateSeats(saved);

        List<SeatMapResponse> created = saved.stream()
                .map(SeatMapMapper::toResponse)
                .collect(Collectors.toList());
        return SeatMapBulkCreateResponse.builder().created(created).skipped(skipped).build();
    }

    private static SeatMapBulkCreateResponse.SkippedRequest skip(SeatMapRequest request, String reason) {
        return SeatMapBulkCreateResponse.SkippedRequest.builder().request(request).reason(reason).build();
    }

    @Override
    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMapById(Long userId, Long id) {
        SeatMap seatMap = seatMapRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_MAP_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, seatMap.getAirlineId(), SeatPermissions.SEAT_READ);
        return SeatMapMapper.toResponse(seatMap);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMapsByCabinClass(Long userId, Long cabinClassId) {
        cabinClassRepository.findById(cabinClassId).orElseThrow(
                () -> new ResourceNotFoundException(String.format(ErrorMessageUtil.CABIN_CLASS_NOT_FOUND_BY_ID, cabinClassId))
        );

        SeatMap seatMap = seatMapRepository.findByCabinClassId(cabinClassId);
        if (seatMap == null) {
            throw new ResourceNotFoundException(
                    String.format(ErrorMessageUtil.SEAT_MAP_NOT_FOUND_BY_CABIN_CLASS_ID, cabinClassId));
        }

        airlineIntegrationService.requirePermission(userId, seatMap.getAirlineId(), SeatPermissions.SEAT_READ);
        return SeatMapMapper.toResponse(seatMap);
    }

    @Override
    public SeatMapResponse updateSeatMap(Long userId, Long id, SeatMapRequest request) {
        SeatMap existing = seatMapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_MAP_NOT_FOUND_BY_ID, id)));

        airlineIntegrationService.requirePermission(userId, existing.getAirlineId(), SeatPermissions.SEAT_CONFIG_MANAGE);

        if (seatMapRepository.existsByAirlineIdAndNameAndIdNot(existing.getAirlineId(), request.getName(), id)) {
            throw new ConflictException(
                    String.format(ErrorMessageUtil.SEAT_MAP_NAME_ALREADY_EXISTS, request.getName()));
        }

        SeatMapMapper.updateEntity(request, existing);
        SeatMap saved = seatMapRepository.save(existing);
        return SeatMapMapper.toResponse(saved);
    }

    @Override
    public void deleteSeatMap(Long userId, Long id) {
        SeatMap existing = seatMapRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_MAP_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, existing.getAirlineId(), SeatPermissions.SEAT_CONFIG_MANAGE);
        seatMapRepository.delete(existing);
    }
}
