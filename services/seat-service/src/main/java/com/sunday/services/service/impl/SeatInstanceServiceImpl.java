package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.SeatPermissions;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.request.SeatInstanceRequest;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeatInstanceServiceImpl implements SeatInstanceService {

    private static final Map<SeatAvailabilityStatus, Set<SeatAvailabilityStatus>> LEGAL_FORWARD_TRANSITIONS = Map.of(
            SeatAvailabilityStatus.AVAILABLE, Set.of(SeatAvailabilityStatus.HELD, SeatAvailabilityStatus.BOOKED, SeatAvailabilityStatus.BLOCKED),
            SeatAvailabilityStatus.HELD, Set.of(SeatAvailabilityStatus.BOOKED, SeatAvailabilityStatus.AVAILABLE),
            SeatAvailabilityStatus.BOOKED, Set.of(SeatAvailabilityStatus.OCCUPIED),
            SeatAvailabilityStatus.OCCUPIED, Set.of(),
            SeatAvailabilityStatus.BLOCKED, Set.of()
    );

    private final SeatInstanceRepository seatInstanceRepository;
    private final SeatRepository seatRepository;
    private final FlightInstanceCabinRepository flightInstanceCabinRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    @Value("${seat.hold-duration-minutes:20}")
    private long holdDurationMinutes;

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
    public BigDecimal calculateSeatPrice(Long userId, List<Long> seatInstanceIds) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllById(seatInstanceIds);
        requirePermissionForAirlines(userId, seatInstances, SeatPermissions.SEAT_READ);
        return sumPremiums(seatInstances);
    }

    @Override
    public List<SeatInstanceResponse> getAllByIdsInternal(List<Long> ids) {
        return seatInstanceRepository.findAllById(ids).stream()
                .map(SeatInstanceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSeatPriceInternal(List<Long> seatInstanceIds) {
        return sumPremiums(seatInstanceRepository.findAllById(seatInstanceIds));
    }

    private BigDecimal sumPremiums(List<SeatInstance> seatInstances) {
        return seatInstances.stream()
                .map(SeatInstance::getPremiumSurcharge)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

    @Override
    public SeatHoldResponse holdSeats(SeatHoldRequest request) {
        List<Long> ids = request.getSeatInstanceIds().stream().distinct().sorted().toList();
        List<SeatInstance> seats = seatInstanceRepository.findAllByIdForUpdate(ids);
        Map<Long, SeatInstance> byId = seats.stream().collect(Collectors.toMap(SeatInstance::getId, s -> s));

        List<String> failures = new ArrayList<>();
        for (Long id : ids) {
            SeatInstance si = byId.get(id);
            boolean alreadyHeldByThisBooking = si != null && si.getStatus() == SeatAvailabilityStatus.HELD
                    && Objects.equals(si.getHeldByBookingReference(), request.getBookingReference());
            if (si == null) {
                failures.add(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_FOUND_BY_ID, id));
            } else if (!Objects.equals(si.getFlightInstanceId(), request.getFlightInstanceId())) {
                failures.add(String.format(ErrorMessageUtil.SEAT_INSTANCE_WRONG_FLIGHT_INSTANCE, id, request.getFlightInstanceId()));
            } else if (si.getFlightInstanceCabin() == null
                    || si.getFlightInstanceCabin().getCabinClass().getName() != request.getCabinClass()) {
                failures.add(String.format(ErrorMessageUtil.SEAT_INSTANCE_WRONG_CABIN_CLASS, id, request.getCabinClass()));
            } else if (si.getStatus() != SeatAvailabilityStatus.AVAILABLE && !alreadyHeldByThisBooking) {
                // A retry of the same booking's hold call (e.g. after a Feign timeout) must not fail just
                // because this seat is already HELD by that exact bookingReference — that's success, not a conflict.
                failures.add(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_AVAILABLE_FOR_HOLD, id));
            }
        }
        if (!failures.isEmpty()) {
            throw new ConflictException(String.join("; ", failures));
        }

        Instant holdExpiresAt = Instant.now().plus(Duration.ofMinutes(holdDurationMinutes));
        BigDecimal totalPremium = BigDecimal.ZERO;
        for (SeatInstance si : seats) {
            si.setStatus(SeatAvailabilityStatus.HELD);
            si.setHeldUntil(holdExpiresAt);
            si.setHeldByBookingReference(request.getBookingReference());
            totalPremium = totalPremium.add(si.getPremiumSurcharge() != null ? si.getPremiumSurcharge() : BigDecimal.ZERO);
        }
        seatInstanceRepository.saveAll(seats);

        return SeatHoldResponse.builder().totalPremium(totalPremium).holdExpiresAt(holdExpiresAt).build();
    }

    @Override
    public void markSeatsBookedBatch(String bookingReference, List<Long> seatInstanceIds) {
        List<Long> ids = seatInstanceIds.stream().distinct().sorted().toList();
        List<SeatInstance> seats = seatInstanceRepository.findAllByIdForUpdate(ids);
        Map<Long, SeatInstance> byId = seats.stream().collect(Collectors.toMap(SeatInstance::getId, s -> s));

        for (Long id : ids) {
            SeatInstance si = byId.get(id);
            if (si == null) {
                log.error("Seat instance {} not found while confirming booking {}", id, bookingReference);
                continue;
            }
            if (si.getStatus() == SeatAvailabilityStatus.HELD || si.getStatus() == SeatAvailabilityStatus.AVAILABLE) {
                si.setStatus(SeatAvailabilityStatus.BOOKED);
                si.setHeldUntil(null);
                si.setHeldByBookingReference(null);
            } else {
                log.error("Seat instance {} is {} (expected HELD or AVAILABLE) while confirming booking {} — " +
                                "the hold likely expired and the seat was taken by someone else",
                        id, si.getStatus(), bookingReference);
            }
        }
        seatInstanceRepository.saveAll(seats);
    }

    @Override
    public void releaseSeatsBatch(List<Long> seatInstanceIds) {
        if (seatInstanceIds == null || seatInstanceIds.isEmpty()) {
            return;
        }
        List<Long> ids = seatInstanceIds.stream().distinct().sorted().toList();
        List<SeatInstance> seats = seatInstanceRepository.findAllByIdForUpdate(ids);
        for (SeatInstance si : seats) {
            if (si.getStatus() == SeatAvailabilityStatus.HELD || si.getStatus() == SeatAvailabilityStatus.BOOKED) {
                si.setStatus(SeatAvailabilityStatus.AVAILABLE);
                si.setHeldUntil(null);
                si.setHeldByBookingReference(null);
            }
        }
        seatInstanceRepository.saveAll(seats);
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
