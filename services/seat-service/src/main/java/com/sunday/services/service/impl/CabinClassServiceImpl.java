package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.constants.SeatPermissions;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.CabinClassRequest;
import com.sunday.common_lib.payload.response.CabinClassBulkCreateResponse;
import com.sunday.common_lib.payload.response.CabinClassResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.integration.AirlineIntegrationService;
import com.sunday.services.mapper.CabinClassMapper;
import com.sunday.services.model.CabinClass;
import com.sunday.services.repository.CabinClassRepository;
import com.sunday.services.service.CabinClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CabinClassServiceImpl implements CabinClassService {

    private final CabinClassRepository cabinClassRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    public CabinClassResponse createCabinClass(UUID userId, CabinClassRequest request) {
        UUID airlineId = airlineIntegrationService.resolveAirlineIdForAircraft(request.getAircraftId());
        airlineIntegrationService.requirePermission(userId, airlineId, SeatPermissions.SEAT_CONFIG_MANAGE);

        if (cabinClassRepository.existsByCodeAndAircraftId(
                request.getCode().toUpperCase(),
                        request.getAircraftId())) {
            throw new ConflictException(
                    String.format(ErrorMessageUtil.CABIN_CLASS_CODE_ALREADY_EXISTS_FOR_AIRCRAFT, request.getCode()));
        }
        CabinClass cabinClass = CabinClassMapper.toEntity(request);
        cabinClass.setAircraftId(request.getAircraftId());
        CabinClass saved = cabinClassRepository.save(cabinClass);
        return CabinClassMapper.toResponse(saved, null);
    }

    @Override
    public CabinClassBulkCreateResponse createCabinClasses(UUID userId, List<CabinClassRequest> requests) {
        if (requests.isEmpty()) {
            return CabinClassBulkCreateResponse.builder().created(List.of()).skipped(List.of()).build();
        }

        Set<UUID> aircraftIds = requests.stream().map(CabinClassRequest::getAircraftId).collect(Collectors.toSet());
        Map<UUID, UUID> airlineIdByAircraft = airlineIntegrationService.resolveAirlineIdsForAircrafts(aircraftIds);

        List<CabinClassBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();
        List<CabinClassRequest> resolvable = new ArrayList<>();
        for (CabinClassRequest req : requests) {
            if (airlineIdByAircraft.containsKey(req.getAircraftId())) {
                resolvable.add(req);
            } else {
                skipped.add(skip(req, String.format(ErrorMessageUtil.AIRCRAFT_NOT_FOUND_BY_ID, req.getAircraftId())));
            }
        }

        Set<UUID> airlineIds = resolvable.stream()
                .map(req -> airlineIdByAircraft.get(req.getAircraftId()))
                .collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, SeatPermissions.SEAT_CONFIG_MANAGE);

        Set<String> existingKeys = resolvable.isEmpty()
                ? Set.of()
                : cabinClassRepository.findByAircraftIdIn(resolvable.stream()
                        .map(CabinClassRequest::getAircraftId).collect(Collectors.toSet()))
                .stream()
                .map(cc -> duplicateKey(cc.getAircraftId(), cc.getCode()))
                .collect(Collectors.toSet());

        Set<String> seenKeys = new HashSet<>();
        List<CabinClass> toSave = new ArrayList<>();
        for (CabinClassRequest req : resolvable) {
            String key = duplicateKey(req.getAircraftId(), req.getCode());
            if (existingKeys.contains(key)) {
                skipped.add(skip(req, String.format(ErrorMessageUtil.CABIN_CLASS_CODE_ALREADY_EXISTS_FOR_AIRCRAFT, req.getCode())));
            } else if (!seenKeys.add(key)) {
                skipped.add(skip(req, String.format(ErrorMessageUtil.CABIN_CLASS_CODE_DUPLICATE_IN_REQUEST, req.getCode())));
            } else {
                CabinClass cc = CabinClassMapper.toEntity(req);
                cc.setAircraftId(req.getAircraftId());
                toSave.add(cc);
            }
        }

        List<CabinClassResponse> created = cabinClassRepository.saveAll(toSave).stream()
                .map(cc -> CabinClassMapper.toResponse(cc, null))
                .collect(Collectors.toList());
        return CabinClassBulkCreateResponse.builder().created(created).skipped(skipped).build();
    }

    private static String duplicateKey(UUID aircraftId, String code) {
        return aircraftId + ":" + code.toUpperCase();
    }

    private static CabinClassBulkCreateResponse.SkippedRequest skip(CabinClassRequest request, String reason) {
        return CabinClassBulkCreateResponse.SkippedRequest.builder().request(request).reason(reason).build();
    }

    @Override
    @Transactional(readOnly = true)
    public CabinClassResponse getCabinClassById(UUID userId, UUID id) {
        CabinClass cabinClass = cabinClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.CABIN_CLASS_NOT_FOUND_BY_ID, id)));
        UUID airlineId = airlineIntegrationService.resolveAirlineIdForAircraft(cabinClass.getAircraftId());
        airlineIntegrationService.requirePermission(userId, airlineId, SeatPermissions.SEAT_READ);
        return CabinClassMapper.toResponse(cabinClass, cabinClass.getSeatMap());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CabinClassResponse> getCabinClassesByAircraftId(UUID userId, UUID aircraftId) {
        UUID airlineId = airlineIntegrationService.resolveAirlineIdForAircraft(aircraftId);
        airlineIntegrationService.requirePermission(userId, airlineId, SeatPermissions.SEAT_READ);
        return cabinClassRepository.findByAircraftId(aircraftId).stream()
                .map(cc -> CabinClassMapper.toResponse(cc, cc.getSeatMap()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CabinClassResponse getByAircraftIdAndName(UUID userId, UUID aircraftId, CabinClassType name) {
        UUID airlineId = airlineIntegrationService.resolveAirlineIdForAircraft(aircraftId);
        airlineIntegrationService.requirePermission(userId, airlineId, SeatPermissions.SEAT_READ);
        CabinClass cabinClass = cabinClassRepository.findByAircraftIdAndName(aircraftId, name);
        return CabinClassMapper.toResponse(cabinClass, null);
    }

    @Override
    public CabinClassResponse updateCabinClass(UUID userId, UUID id, CabinClassRequest request) {
        CabinClass existing = cabinClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.CABIN_CLASS_NOT_FOUND_BY_ID, id)));

        UUID currentAirlineId = airlineIntegrationService.resolveAirlineIdForAircraft(existing.getAircraftId());
        airlineIntegrationService.requirePermission(userId, currentAirlineId, SeatPermissions.SEAT_CONFIG_MANAGE);

        if (request.getAircraftId() != null && !request.getAircraftId().equals(existing.getAircraftId())) {
            UUID destinationAirlineId = airlineIntegrationService.resolveAirlineIdForAircraft(request.getAircraftId());
            airlineIntegrationService.requirePermission(userId, destinationAirlineId, SeatPermissions.SEAT_CONFIG_MANAGE);
        }

        if (cabinClassRepository.existsByCodeAndAircraftIdAndIdNot(
                request.getCode().toUpperCase(), existing.getAircraftId(), id)) {
            throw new ConflictException(
                    String.format(ErrorMessageUtil.CABIN_CLASS_CODE_ALREADY_EXISTS_FOR_AIRCRAFT, request.getCode()));
        }

        CabinClassMapper.updateEntity(request, existing);
        CabinClass saved = cabinClassRepository.save(existing);
        return CabinClassMapper.toResponse(saved, saved.getSeatMap());
    }

    @Override
    public void deleteCabinClass(UUID userId, UUID id) {
        CabinClass cabinClass = cabinClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.CABIN_CLASS_NOT_FOUND_BY_ID, id)));
        UUID airlineId = airlineIntegrationService.resolveAirlineIdForAircraft(cabinClass.getAircraftId());
        airlineIntegrationService.requirePermission(userId, airlineId, SeatPermissions.SEAT_CONFIG_MANAGE);
        cabinClassRepository.delete(cabinClass);
    }
}
