package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.event.FlightInstanceAircraftChangedEvent;
import com.sunday.common_lib.event.FlightInstanceCreatedEvent;
import com.sunday.common_lib.event.FlightInstancesBulkCreatedEvent;
import com.sunday.services.model.CabinClass;
import com.sunday.services.model.FlightInstanceCabin;
import com.sunday.services.model.Seat;
import com.sunday.services.model.SeatInstance;
import com.sunday.services.repository.CabinClassRepository;
import com.sunday.services.repository.FlightInstanceCabinRepository;
import com.sunday.services.repository.SeatInstanceRepository;
import com.sunday.services.repository.SeatRepository;
import com.sunday.services.service.CabinInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CabinInventoryServiceImpl implements CabinInventoryService {

    private final CabinClassRepository cabinClassRepository;
    private final SeatRepository seatRepository;
    private final FlightInstanceCabinRepository flightInstanceCabinRepository;
    private final SeatInstanceRepository seatInstanceRepository;

    @Override
    public void materializeForFlightInstance(FlightInstanceCreatedEvent event) {
        log.info("Received FlightInstanceCreatedEvent: flightInstanceId={}, aircraftId={}",
                event.getFlightInstanceId(), event.getAircraftId());

        if (!flightInstanceCabinRepository.findAllByFlightInstanceId(event.getFlightInstanceId()).isEmpty()) {
            log.info("FlightInstanceCabins already exist for flightInstanceId={} — skipping (redelivered event)",
                    event.getFlightInstanceId());
            return;
        }

        materializeCabinsAndSeats(event.getAircraftId(), event.getFlightId(), List.of(event.getFlightInstanceId()));
    }

    @Override
    public void materializeForFlightInstances(FlightInstancesBulkCreatedEvent event) {
        List<UUID> requestedIds = event.getFlightInstanceIds();
        if (requestedIds == null || requestedIds.isEmpty()) {
            return;
        }
        log.info("Received FlightInstancesBulkCreatedEvent: flightId={}, aircraftId={}, count={}",
                event.getFlightId(), event.getAircraftId(), requestedIds.size());

        Set<UUID> alreadyProcessed = flightInstanceCabinRepository.findAllByFlightInstanceIdIn(requestedIds).stream()
                .map(FlightInstanceCabin::getFlightInstanceId)
                .collect(Collectors.toSet());
        List<UUID> pendingIds = requestedIds.stream().filter(id -> !alreadyProcessed.contains(id)).toList();

        if (pendingIds.isEmpty()) {
            log.info("Bulk event for flightId={} aircraftId={}: all {} instances already processed — skipping",
                    event.getFlightId(), event.getAircraftId(), requestedIds.size());
            return;
        }

        materializeCabinsAndSeats(event.getAircraftId(), event.getFlightId(), pendingIds);
    }

    @Override
    public void reconcileForAircraftChange(FlightInstanceAircraftChangedEvent event) {
        log.info("Received FlightInstanceAircraftChangedEvent: flightInstanceId={}, oldAircraftId={}, newAircraftId={}",
                event.getFlightInstanceId(), event.getOldAircraftId(), event.getNewAircraftId());

        List<CabinClass> newCabinClasses = cabinClassRepository.findByAircraftId(event.getNewAircraftId());
        List<FlightInstanceCabin> existingCabins =
                flightInstanceCabinRepository.findAllByFlightInstanceId(event.getFlightInstanceId());
        Map<CabinClassType, FlightInstanceCabin> existingByType = existingCabins.stream()
                .collect(Collectors.toMap(fic -> fic.getCabinClass().getName(), fic -> fic, (a, b) -> a));
        Set<CabinClassType> newTypes = newCabinClasses.stream().map(CabinClass::getName).collect(Collectors.toSet());

        for (CabinClass cabinClass : newCabinClasses) {
            List<Seat> desiredSeats = cabinClass.getSeatMap() != null
                    ? seatRepository.findBySeatMapId(cabinClass.getSeatMap().getId())
                    : List.of();
            int desiredCount = desiredSeats.size();

            FlightInstanceCabin fic = existingByType.get(cabinClass.getName());
            if (fic == null) {
                FlightInstanceCabin savedFic = flightInstanceCabinRepository.save(FlightInstanceCabin.builder()
                        .flightInstanceId(event.getFlightInstanceId())
                        .cabinClass(cabinClass)
                        .totalSeats(desiredCount)
                        .build());
                seatInstanceRepository.saveAll(buildAvailableSeatInstances(
                        desiredSeats, savedFic, event.getFlightId(), event.getFlightInstanceId()));
                continue;
            }

            fic.setCabinClass(cabinClass);
            fic.setTotalSeats(desiredCount);
            flightInstanceCabinRepository.save(fic);
            reconcileCabinSeats(fic, desiredSeats, desiredCount, event.getFlightId(), event.getFlightInstanceId());
        }

        for (FlightInstanceCabin fic : existingCabins) {
            if (!newTypes.contains(fic.getCabinClass().getName())) {
                flightInstanceCabinRepository.delete(fic);
            }
        }

        log.info("Reconciled cabins/seats for flightInstanceId={} to aircraftId={}",
                event.getFlightInstanceId(), event.getNewAircraftId());
    }

    private void materializeCabinsAndSeats(UUID aircraftId, UUID flightId, List<UUID> flightInstanceIds) {
        List<CabinClass> cabinClasses = cabinClassRepository.findByAircraftId(aircraftId);

        Map<UUID, List<Seat>> seatsByCabinClassId = new HashMap<>();
        for (CabinClass cabinClass : cabinClasses) {
            List<Seat> seats = cabinClass.getSeatMap() != null
                    ? seatRepository.findBySeatMapId(cabinClass.getSeatMap().getId())
                    : List.of();
            seatsByCabinClassId.put(cabinClass.getId(), seats);
        }

        List<FlightInstanceCabin> cabinsToSave = new ArrayList<>();
        for (UUID flightInstanceId : flightInstanceIds) {
            for (CabinClass cabinClass : cabinClasses) {
                cabinsToSave.add(FlightInstanceCabin.builder()
                        .flightInstanceId(flightInstanceId)
                        .cabinClass(cabinClass)
                        .totalSeats(seatsByCabinClassId.get(cabinClass.getId()).size())
                        .build());
            }
        }
        List<FlightInstanceCabin> savedCabins = flightInstanceCabinRepository.saveAll(cabinsToSave);

        List<SeatInstance> seatInstancesToSave = new ArrayList<>();
        for (FlightInstanceCabin fic : savedCabins) {
            seatInstancesToSave.addAll(buildAvailableSeatInstances(
                    seatsByCabinClassId.get(fic.getCabinClass().getId()), fic, flightId, fic.getFlightInstanceId()));
        }
        seatInstanceRepository.saveAll(seatInstancesToSave);

        log.info("Materialized {} FlightInstanceCabin and {} SeatInstance records for {} flight instance(s) (aircraftId={})",
                savedCabins.size(), seatInstancesToSave.size(), flightInstanceIds.size(), aircraftId);
    }

    /**
     * Rebuilds a cabin's seats on the new aircraft's seat map. Unsold seats are dropped and recreated from the new
     * seats. Sold or held seats keep their row (so bookings keep their seat instance id and price) but are moved onto
     * the new aircraft's seat with the same number, or the first free one if that number does not exist there.
     */
    private void reconcileCabinSeats(FlightInstanceCabin fic, List<Seat> desiredSeats, int desiredCount,
                                     UUID flightId, UUID flightInstanceId) {
        List<SeatInstance> currentSeats = seatInstanceRepository.findByFlightInstanceCabinId(fic.getId());
        List<SeatInstance> unsold = currentSeats.stream()
                .filter(si -> si.getStatus() == SeatAvailabilityStatus.AVAILABLE)
                .toList();
        List<SeatInstance> sold = currentSeats.stream()
                .filter(si -> si.getStatus() != SeatAvailabilityStatus.AVAILABLE)
                .toList();
        seatInstanceRepository.deleteAll(unsold);
        seatInstanceRepository.flush();

        Map<String, Seat> newSeatsByNumber = new LinkedHashMap<>();
        desiredSeats.forEach(seat -> newSeatsByNumber.putIfAbsent(seat.getSeatNumber(), seat));
        Set<UUID> taken = new HashSet<>();
        for (SeatInstance si : sold) {
            Seat target = newSeatsByNumber.get(si.getSeat().getSeatNumber());
            if (target == null || taken.contains(target.getId())) {
                target = desiredSeats.stream().filter(seat -> !taken.contains(seat.getId())).findFirst().orElse(null);
            }
            if (target == null) {
                log.error("No seat left on the new aircraft for sold seat instance {} of flight instance {}",
                        si.getId(), flightInstanceId);
                continue;
            }
            taken.add(target.getId());
            si.setSeat(target);
        }
        seatInstanceRepository.saveAll(sold);
        seatInstanceRepository.flush();

        List<Seat> free = desiredSeats.stream().filter(seat -> !taken.contains(seat.getId())).toList();
        seatInstanceRepository.saveAll(buildAvailableSeatInstances(free, fic, flightId, flightInstanceId));

        fic.setTotalSeats(desiredCount);
        flightInstanceCabinRepository.save(fic);
    }

    private List<SeatInstance> buildAvailableSeatInstances(List<Seat> seats, FlightInstanceCabin fic,
                                                           UUID flightId, UUID flightInstanceId) {
        return seats.stream()
                .map(seat -> SeatInstance.builder()
                        .flightId(flightId)
                        .flightInstanceId(flightInstanceId)
                        .flightInstanceCabin(fic)
                        .seat(seat)
                        .status(SeatAvailabilityStatus.AVAILABLE)
                        .premiumSurcharge(seat.getPremiumSurcharge())
                        .build())
                .toList();
    }
}
