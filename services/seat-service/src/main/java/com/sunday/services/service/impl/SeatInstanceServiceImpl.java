package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.SeatPermissions;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.CabinAvailabilityRequest;
import com.sunday.common_lib.payload.request.SeatConfirmRequest;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.request.SeatInstanceRequest;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.CustomerSeatResponse;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.common_lib.payload.response.SeatConfirmResponse;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.integration.AirlineIntegrationService;
import com.sunday.services.integration.FlightInstanceGuard;
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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeatInstanceServiceImpl implements SeatInstanceService {

    /**
     * What staff may set by hand. HELD is never set or left by hand: only the booking flow holds a seat (with an
     * expiry and an owner), and only release, the sweeper or a cancellation return one to AVAILABLE.
     */
    private static final Map<SeatAvailabilityStatus, Set<SeatAvailabilityStatus>> LEGAL_FORWARD_TRANSITIONS = Map.of(
            SeatAvailabilityStatus.AVAILABLE, Set.of(SeatAvailabilityStatus.BOOKED, SeatAvailabilityStatus.BLOCKED),
            SeatAvailabilityStatus.HELD, Set.of(),
            SeatAvailabilityStatus.BOOKED, Set.of(SeatAvailabilityStatus.OCCUPIED),
            SeatAvailabilityStatus.OCCUPIED, Set.of(),
            SeatAvailabilityStatus.BLOCKED, Set.of()
    );

    private final SeatInstanceRepository seatInstanceRepository;
    private final SeatRepository seatRepository;
    private final FlightInstanceCabinRepository flightInstanceCabinRepository;
    private final AirlineIntegrationService airlineIntegrationService;
    private final FlightInstanceGuard flightInstanceGuard;

    @Value("${seat.hold-duration-minutes:20}")
    private long holdDurationMinutes;

    @Override
    public SeatInstanceResponse createSeatInstance(UUID userId, SeatInstanceRequest request) {
        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.SEAT_NOT_FOUND_BY_ID, request.getSeatId())));
        airlineIntegrationService.requirePermission(
                userId, seat.getSeatMap().getAirlineId(), SeatPermissions.SEAT_INSTANCE_CONFIG_MANAGE);
        FlightInstanceResponse flightInstance =
                flightInstanceGuard.requireOperatedBy(request.getFlightInstanceId(), seat.getSeatMap().getCabinClass());
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
        seatInstance.setFlightId(flightInstance.getFlightId());
        return SeatInstanceMapper.toResponse(seatInstanceRepository.save(seatInstance));
    }

    @Override
    @Transactional(readOnly = true)
    public SeatInstanceResponse getSeatInstanceById(UUID userId, UUID id) {
        SeatInstance si = seatInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, si.getSeat().getSeatMap().getAirlineId(), SeatPermissions.SEAT_READ);
        return SeatInstanceMapper.toResponse(si);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatInstanceResponse> getSeatInstancesByFlightId(UUID userId, UUID flightId) {
        List<SeatInstance> instances = seatInstanceRepository.findByFlightId(flightId);
        requirePermissionForFirst(userId, instances, SeatPermissions.SEAT_READ);
        return instances.stream().map(SeatInstanceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatInstanceResponse> getAvailableSeatsByFlightId(UUID userId, UUID flightId) {
        List<SeatInstance> instances = seatInstanceRepository.findAvailableByFlightId(flightId);
        requirePermissionForFirst(userId, instances, SeatPermissions.SEAT_READ);
        return instances.stream().map(SeatInstanceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatInstanceResponse> getAllByIds(UUID userId, List<UUID> ids) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllById(ids);
        requirePermissionForAirlines(userId, seatInstances, SeatPermissions.SEAT_READ);
        return seatInstances.stream().map(SeatInstanceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long countAvailableByFlightId(UUID userId, UUID flightId) {
        seatInstanceRepository.findFirstByFlightId(flightId).ifPresent(any -> airlineIntegrationService.requirePermission(
                userId, any.getSeat().getSeatMap().getAirlineId(), SeatPermissions.SEAT_READ));
        return seatInstanceRepository.countAvailableByFlightId(flightId);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSeatPrice(UUID userId, List<UUID> seatInstanceIds) {
        List<SeatInstance> seatInstances = seatInstanceRepository.findAllById(seatInstanceIds);
        requirePermissionForAirlines(userId, seatInstances, SeatPermissions.SEAT_READ);
        return sumPremiums(seatInstances);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatInstanceResponse> getAllByIdsInternal(List<UUID> ids) {
        return seatInstanceRepository.findAllById(ids).stream()
                .map(SeatInstanceMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateSeatPriceInternal(List<UUID> seatInstanceIds) {
        return sumPremiums(seatInstanceRepository.findAllById(seatInstanceIds));
    }

    @Override
    public SeatInstanceResponse updateSeatInstanceStatus(UUID userId, UUID id, SeatAvailabilityStatus status) {
        SeatInstance si = seatInstanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, si.getSeat().getSeatMap().getAirlineId(), SeatPermissions.SEAT_INSTANCE_STATUS_MANAGE);
        requireLegalForwardTransition(si, status);
        si.setStatus(status);
        return SeatInstanceMapper.toResponse(seatInstanceRepository.save(si));
    }

    @Override
    public SeatInstanceResponse releaseSeatInstance(UUID userId, UUID id) {
        SeatInstance si = seatInstanceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, si.getSeat().getSeatMap().getAirlineId(), SeatPermissions.SEAT_INSTANCE_RELEASE);
        if (si.getStatus() == SeatAvailabilityStatus.AVAILABLE) {
            throw new ConflictException(String.format(ErrorMessageUtil.SEAT_INSTANCE_ILLEGAL_STATUS_TRANSITION,
                    id, si.getStatus(), SeatAvailabilityStatus.AVAILABLE));
        }
        makeAvailable(si);
        return SeatInstanceMapper.toResponse(seatInstanceRepository.save(si));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> countAvailableSeats(CabinAvailabilityRequest request) {
        Map<UUID, Long> counts = new HashMap<>();
        request.getFlightInstanceIds().forEach(id -> counts.put(id, 0L));
        seatInstanceRepository.countAvailableInCabin(counts.keySet(), request.getCabinClass())
                .forEach(row -> counts.put(row.getFlightInstanceId(), row.getAvailableSeats()));
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, Long> countSoldSeats(List<UUID> flightInstanceIds) {
        Map<UUID, Long> counts = new HashMap<>();
        flightInstanceIds.forEach(id -> counts.put(id, 0L));
        if (!counts.isEmpty()) {
            seatInstanceRepository.countSoldByFlightInstance(counts.keySet())
                    .forEach(row -> counts.put(row.getFlightInstanceId(), row.getSoldSeats()));
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CabinSeatStatusResponse> getSeatStatusSummary(UUID flightInstanceId) {
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
    @Transactional(readOnly = true)
    public List<CustomerSeatResponse> getCustomerSeatMap(UUID flightInstanceId) {
        return seatInstanceRepository.findSeatMapByFlightInstanceId(flightInstanceId).stream()
                .map(SeatInstanceMapper::toCustomerResponse)
                .toList();
    }

    @Override
    public SeatHoldResponse holdSeats(SeatHoldRequest request) {
        List<UUID> ids = request.getSeatInstanceIds().stream().distinct().sorted().toList();
        List<SeatInstance> seats = seatInstanceRepository.findAllByIdForUpdate(ids);
        Map<UUID, SeatInstance> byId = seats.stream().collect(Collectors.toMap(SeatInstance::getId, s -> s));

        List<String> failures = new ArrayList<>();
        for (UUID id : ids) {
            SeatInstance si = byId.get(id);
            boolean alreadyHeldByThisBooking = si != null && si.getStatus() == SeatAvailabilityStatus.HELD
                    && Objects.equals(si.getBookingReference(), request.getBookingReference());
            if (si == null) {
                failures.add(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_FOUND_BY_ID, id));
            } else if (!Objects.equals(si.getFlightInstanceId(), request.getFlightInstanceId())) {
                failures.add(String.format(ErrorMessageUtil.SEAT_INSTANCE_WRONG_FLIGHT_INSTANCE, id, request.getFlightInstanceId()));
            } else if (!isInCabin(si, request.getCabinClass())) {
                failures.add(String.format(ErrorMessageUtil.SEAT_INSTANCE_WRONG_CABIN_CLASS, id, request.getCabinClass()));
            } else if (si.getStatus() != SeatAvailabilityStatus.AVAILABLE && !alreadyHeldByThisBooking) {
                // A retry of the same booking's hold (e.g. after a Feign timeout) finds its own HELD seats: success.
                failures.add(String.format(ErrorMessageUtil.SEAT_INSTANCE_NOT_AVAILABLE_FOR_HOLD, id));
            }
        }
        if (!failures.isEmpty()) {
            throw new ConflictException(String.join("; ", failures));
        }

        Instant holdExpiresAt = Instant.now().plus(Duration.ofMinutes(holdDurationMinutes));
        for (SeatInstance si : seats) {
            si.setStatus(SeatAvailabilityStatus.HELD);
            si.setHeldUntil(holdExpiresAt);
            si.setBookingReference(request.getBookingReference());
        }
        seatInstanceRepository.saveAll(seats);
        return SeatHoldResponse.builder().totalPremium(sumPremiums(seats)).holdExpiresAt(holdExpiresAt).build();
    }

    @Override
    public SeatConfirmResponse confirmSeats(SeatConfirmRequest request) {
        String reference = request.getBookingReference();
        List<UUID> requested = request.getSeatInstanceIds().stream().distinct().toList();
        Map<UUID, SeatInstance> byId = seatInstanceRepository
                .findAllByIdForUpdate(requested.stream().sorted().toList()).stream()
                .collect(Collectors.toMap(SeatInstance::getId, s -> s));

        List<SeatInstance> keep = new ArrayList<>();
        List<UUID> lost = new ArrayList<>();
        for (UUID id : requested) {
            SeatInstance si = byId.get(id);
            if (si != null && Objects.equals(si.getFlightInstanceId(), request.getFlightInstanceId())
                    && isInCabin(si, request.getCabinClass()) && isFreeOrOwnedBy(si, reference)) {
                keep.add(si);
            } else {
                lost.add(id);
            }
        }

        Map<UUID, UUID> reassigned = new LinkedHashMap<>();
        if (!lost.isEmpty()) {
            Set<UUID> kept = keep.stream().map(SeatInstance::getId).collect(Collectors.toSet());
            Deque<SeatInstance> replacements = replacementCandidates(request, kept);
            if (replacements.size() < lost.size()) {
                log.warn("Booking {} lost {} seat(s) on flight instance {} and the {} cabin has no free seat left",
                        reference, lost.size(), request.getFlightInstanceId(), request.getCabinClass());
                releaseOwnedSeats(reference, keep);
                return SeatConfirmResponse.builder().allocated(false).seatInstanceIds(List.of()).reassigned(Map.of()).build();
            }
            for (UUID lostId : lost) {
                SeatInstance replacement = replacements.removeFirst();
                reassigned.put(lostId, replacement.getId());
                keep.add(replacement);
            }
            log.warn("Booking {} lost seat(s) {}; reassigned to {}", reference, lost, reassigned.values());
        }

        for (SeatInstance si : keep) {
            si.setStatus(SeatAvailabilityStatus.BOOKED);
            si.setHeldUntil(null);
            si.setBookingReference(reference);
        }
        seatInstanceRepository.saveAll(keep);

        List<UUID> finalIds = requested.stream().map(id -> reassigned.getOrDefault(id, id)).toList();
        return SeatConfirmResponse.builder().allocated(true).seatInstanceIds(finalIds).reassigned(reassigned).build();
    }

    @Override
    public void releaseSeatsForBooking(String bookingReference, List<UUID> seatInstanceIds) {
        if (seatInstanceIds == null || seatInstanceIds.isEmpty()) {
            return;
        }
        List<SeatInstance> seats = seatInstanceRepository.findAllByIdForUpdate(seatInstanceIds.stream().distinct().sorted().toList());
        releaseOwnedSeats(bookingReference, seats);
    }

    // ---------- Helpers ----------

    /**
     * Seats this booking already owns in the cabin but did not ask for come first (left over from an earlier attempt
     * whose result booking-service never saved), then free seats, so a retried confirmation never books extra seats.
     */
    private Deque<SeatInstance> replacementCandidates(SeatConfirmRequest request, Set<UUID> excluded) {
        Deque<SeatInstance> candidates = new ArrayDeque<>();
        Set<UUID> seen = new HashSet<>(excluded);
        for (SeatInstance si : seatInstanceRepository.findOwnedInCabinForUpdate(
                request.getFlightInstanceId(), request.getCabinClass(), request.getBookingReference())) {
            if (seen.add(si.getId())) {
                candidates.add(si);
            }
        }
        for (SeatInstance si : seatInstanceRepository.findAvailableInCabinForUpdate(
                request.getFlightInstanceId(), request.getCabinClass())) {
            if (seen.add(si.getId())) {
                candidates.add(si);
            }
        }
        return candidates;
    }

    private void releaseOwnedSeats(String bookingReference, List<SeatInstance> seats) {
        List<SeatInstance> owned = seats.stream()
                .filter(si -> si.getStatus() == SeatAvailabilityStatus.HELD || si.getStatus() == SeatAvailabilityStatus.BOOKED)
                .filter(si -> Objects.equals(si.getBookingReference(), bookingReference))
                .toList();
        owned.forEach(SeatInstanceServiceImpl::makeAvailable);
        seatInstanceRepository.saveAll(owned);
    }

    private static boolean isFreeOrOwnedBy(SeatInstance si, String bookingReference) {
        if (si.getStatus() == SeatAvailabilityStatus.AVAILABLE) {
            return true;
        }
        return (si.getStatus() == SeatAvailabilityStatus.HELD || si.getStatus() == SeatAvailabilityStatus.BOOKED)
                && Objects.equals(si.getBookingReference(), bookingReference);
    }

    private static boolean isInCabin(SeatInstance si, CabinClassType cabinClass) {
        return si.getFlightInstanceCabin() != null && si.getFlightInstanceCabin().getCabinClass().getName() == cabinClass;
    }

    private static void makeAvailable(SeatInstance si) {
        si.setStatus(SeatAvailabilityStatus.AVAILABLE);
        si.setHeldUntil(null);
        si.setBookingReference(null);
    }

    private static BigDecimal sumPremiums(List<SeatInstance> seatInstances) {
        return seatInstances.stream()
                .map(SeatInstance::getPremiumSurcharge)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void requireLegalForwardTransition(SeatInstance si, SeatAvailabilityStatus target) {
        Set<SeatAvailabilityStatus> legalTargets = LEGAL_FORWARD_TRANSITIONS.getOrDefault(si.getStatus(), Set.of());
        if (!legalTargets.contains(target)) {
            throw new ConflictException(String.format(ErrorMessageUtil.SEAT_INSTANCE_ILLEGAL_STATUS_TRANSITION,
                    si.getId(), si.getStatus(), target));
        }
    }

    private void requirePermissionForFirst(UUID userId, List<SeatInstance> instances, String permission) {
        if (instances.isEmpty()) {
            return;
        }
        airlineIntegrationService.requirePermission(userId, instances.get(0).getSeat().getSeatMap().getAirlineId(), permission);
    }

    private void requirePermissionForAirlines(UUID userId, List<SeatInstance> instances, String permission) {
        Set<UUID> airlineIds = instances.stream()
                .map(si -> si.getSeat().getSeatMap().getAirlineId())
                .collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, permission);
    }
}
