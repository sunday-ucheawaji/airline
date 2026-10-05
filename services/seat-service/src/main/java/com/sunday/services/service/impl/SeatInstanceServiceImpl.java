package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.SeatPermissions;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.SeatInstanceRequest;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.integration.AirlineIntegrationService;
import com.sunday.services.mapper.SeatInstanceMapper;
import com.sunday.services.model.FlightInstanceCabin;
import com.sunday.services.model.Seat;
import com.sunday.services.model.SeatInstance;
import com.sunday.services.repository.FlightInstanceCabinRepository;
import com.sunday.services.repository.SeatInstanceRepository;
import com.sunday.services.repository.SeatRepository;
import com.sunday.services.service.SeatInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatInstanceServiceImpl implements SeatInstanceService {

    private static final Map<SeatAvailabilityStatus, Set<SeatAvailabilityStatus>> LEGAL_FORWARD_TRANSITIONS = Map.of(
            SeatAvailabilityStatus.AVAILABLE, Set.of(SeatAvailabilityStatus.BOOKED, SeatAvailabilityStatus.BLOCKED),
            SeatAvailabilityStatus.BOOKED, Set.of(SeatAvailabilityStatus.OCCUPIED),
            SeatAvailabilityStatus.OCCUPIED, Set.of(),
            SeatAvailabilityStatus.BLOCKED, Set.of()
    );

    private final SeatInstanceRepository seatInstanceRepository;
    private final SeatRepository seatRepository;
    private final FlightInstanceCabinRepository flightInstanceCabinRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    public SeatInstanceResponse createSeatInstance(Long userId, SeatInstanceRequest request) {
        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.SEAT_NOT_FOUND_BY_ID, request.getSeatId())));

        airlineIntegrationService.requirePermission(
                userId, seat.getSeatMap().getAirlineId(), SeatPermissions.SEAT_INSTANCE_CONFIG_MANAGE);

        if (seatInstanceRepository.existsBySeatIdAndFlightInstanceId(request.getSeatId(), request.getFlightInstanceId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.SEAT_INSTANCE_DUPLICATE_FOR_SEAT, request.getSeatId()));
        }

        FlightInstanceCabin fic = null;
        if (request.getFlightInstanceCabinId() != null) {
            fic = flightInstanceCabinRepository.findById(request.getFlightInstanceCabinId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ErrorMessageUtil.FLIGHT_INSTANCE_CABIN_NOT_FOUND_BY_ID, request.getFlightInstanceCabinId())));
        }

        SeatInstance seatInstance = SeatInstanceMapper.toEntity(request, seat, fic);
        SeatInstance saved = seatInstanceRepository.save(seatInstance);
        return SeatInstanceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatInstanceResponse getSeatInstanceById(Long userId, Long id) {
        SeatInstance si = seatInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, si.getSeat().getSeatMap().getAirlineId(), SeatPermissions.SEAT_READ);
        return SeatInstanceMapper.toResponse(si);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatInstanceResponse> getSeatInstancesByFlightId(Long userId, Long flightId) {
        List<SeatInstance> instances = seatInstanceRepository.findByFlightId(flightId);
        requirePermissionForFirst(userId, instances, SeatPermissions.SEAT_READ);
        return instances.stream().map(SeatInstanceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatInstanceResponse> getAvailableSeatsByFlightId(Long userId, Long flightId) {
        List<SeatInstance> instances = seatInstanceRepository.findAvailableByFlightId(flightId);
        requirePermissionForFirst(userId, instances, SeatPermissions.SEAT_READ);
        return instances.stream().map(SeatInstanceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatInstanceResponse> getAllByIds(Long userId, List<Long> ids) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllById(ids);
        requirePermissionForAirlines(userId, seatInstances, SeatPermissions.SEAT_READ);
        return seatInstances.stream().map(SeatInstanceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long countAvailableByFlightId(Long userId, Long flightId) {
        List<SeatInstance> sample = seatInstanceRepository.findByFlightId(flightId);
        requirePermissionForFirst(userId, sample, SeatPermissions.SEAT_READ);
        return seatInstanceRepository.countAvailableByFlightId(flightId);
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateSeatPrice(Long userId, List<Long> seatInstanceIds) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllById(seatInstanceIds);
        requirePermissionForAirlines(userId, seatInstances, SeatPermissions.SEAT_READ);

        double total = 0.0;
        for (SeatInstance si : seatInstances) {
            double seatPremium = si.getPremiumSurcharge() != null ? si.getPremiumSurcharge() : 0.0;
            total += seatPremium;
        }
        return total;
    }

    @Override
    public SeatInstanceResponse updateSeatInstanceStatus(Long userId, Long id, SeatAvailabilityStatus status) {
        SeatInstance si = seatInstanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_FOUND_BY_ID, id)));

        airlineIntegrationService.requirePermission(
                userId, si.getSeat().getSeatMap().getAirlineId(), SeatPermissions.SEAT_INSTANCE_STATUS_MANAGE);

        requireLegalForwardTransition(si, status);
        si.setStatus(status);

        SeatInstance saved = seatInstanceRepository.save(si);
        return SeatInstanceMapper.toResponse(saved);
    }

    @Override
    public SeatInstanceResponse updateSeatInstanceStatusInternal(Long id, SeatAvailabilityStatus status) {
        SeatInstance si = seatInstanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_FOUND_BY_ID, id)));

        si.setStatus(status);

        SeatInstance saved = seatInstanceRepository.save(si);
        return SeatInstanceMapper.toResponse(saved);
    }

    @Override
    public SeatInstanceResponse releaseSeatInstance(Long userId, Long id) {
        SeatInstance si = seatInstanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_FOUND_BY_ID, id)));

        airlineIntegrationService.requirePermission(
                userId, si.getSeat().getSeatMap().getAirlineId(), SeatPermissions.SEAT_INSTANCE_RELEASE);

        if (si.getStatus() == SeatAvailabilityStatus.AVAILABLE) {
            throw new ConflictException(String.format(ErrorMessageUtil.SEAT_INSTANCE_ILLEGAL_STATUS_TRANSITION,
                    id, si.getStatus(), SeatAvailabilityStatus.AVAILABLE));
        }

        si.setStatus(SeatAvailabilityStatus.AVAILABLE);

        SeatInstance saved = seatInstanceRepository.save(si);
        return SeatInstanceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CabinSeatStatusResponse> getSeatStatusSummary(Long flightInstanceId) {
        List<SeatInstance> instances = seatInstanceRepository.findByFlightInstanceId(flightInstanceId);
        Map<CabinClassType, Long> counts = instances.stream()
                .filter(si -> si.getStatus() != SeatAvailabilityStatus.AVAILABLE)
                .filter(si -> si.getFlightInstanceCabin() != null)
                .collect(Collectors.groupingBy(
                        si -> si.getFlightInstanceCabin().getCabinClass().getName(),
                        Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> CabinSeatStatusResponse.builder()
                        .cabinClass(e.getKey())
                        .nonAvailableSeatCount(e.getValue())
                        .build())
                .toList();
    }

    private void requireLegalForwardTransition(SeatInstance si, SeatAvailabilityStatus target) {
        Set<SeatAvailabilityStatus> legalTargets = LEGAL_FORWARD_TRANSITIONS.getOrDefault(si.getStatus(), Set.of());
        if (!legalTargets.contains(target)) {
            throw new ConflictException(String.format(ErrorMessageUtil.SEAT_INSTANCE_ILLEGAL_STATUS_TRANSITION,
                    si.getId(), si.getStatus(), target));
        }
    }

    private void requirePermissionForFirst(Long userId, List<SeatInstance> instances, String permission) {
        if (instances.isEmpty()) {
            return;
        }
        Long airlineId = instances.get(0).getSeat().getSeatMap().getAirlineId();
        airlineIntegrationService.requirePermission(userId, airlineId, permission);
    }

    private void requirePermissionForAirlines(Long userId, List<SeatInstance> instances, String permission) {
        Set<Long> airlineIds = instances.stream()
                .map(si -> si.getSeat().getSeatMap().getAirlineId())
                .collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, permission);
    }
}
