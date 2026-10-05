package com.sunday.services.service.impl;

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
        List<Long> requestedIds = event.getFlightInstanceIds();
        if (requestedIds == null || requestedIds.isEmpty()) {
            return;
        }
        log.info("Received FlightInstancesBulkCreatedEvent: flightId={}, aircraftId={}, count={}",
                event.getFlightId(), event.getAircraftId(), requestedIds.size());

        Set<Long> alreadyProcessed = flightInstanceCabinRepository.findAllByFlightInstanceIdIn(requestedIds).stream()
                .map(FlightInstanceCabin::getFlightInstanceId)
                .collect(Collectors.toSet());
        List<Long> pendingIds = requestedIds.stream().filter(id -> !alreadyProcessed.contains(id)).toList();

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

    private void materializeCabinsAndSeats(Long aircraftId, Long flightId, List<Long> flightInstanceIds) {
        List<CabinClass> cabinClasses = cabinClassRepository.findByAircraftId(aircraftId);

        Map<Long, List<Seat>> seatsByCabinClassId = new HashMap<>();
        for (CabinClass cabinClass : cabinClasses) {
            List<Seat> seats = cabinClass.getSeatMap() != null
                    ? seatRepository.findBySeatMapId(cabinClass.getSeatMap().getId())
                    : List.of();
            seatsByCabinClassId.put(cabinClass.getId(), seats);
        }

        List<FlightInstanceCabin> cabinsToSave = new ArrayList<>();
        for (Long flightInstanceId : flightInstanceIds) {
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

    private void reconcileCabinSeats(FlightInstanceCabin fic, List<Seat> desiredSeats, int desiredCount,
                                     Long flightId, Long flightInstanceId) {
        List<SeatInstance> currentSeats = seatInstanceRepository.findByFlightInstanceCabinId(fic.getId());
        int currentTotal = currentSeats.size();

        if (currentTotal < desiredCount) {
            Set<Long> usedSeatIds = currentSeats.stream().map(si -> si.getSeat().getId()).collect(Collectors.toSet());
            List<Seat> unusedSeats = desiredSeats.stream().filter(s -> !usedSeatIds.contains(s.getId())).toList();
            int toAdd = desiredCount - currentTotal;
            seatInstanceRepository.saveAll(buildAvailableSeatInstances(
                    unusedSeats.stream().limit(toAdd).toList(), fic, flightId, flightInstanceId));
        } else if (currentTotal > desiredCount) {
            int toRemove = currentTotal - desiredCount;
            List<SeatInstance> removable = currentSeats.stream()
                    .filter(si -> si.getStatus() == SeatAvailabilityStatus.AVAILABLE)
                    .limit(toRemove)
                    .toList();
            seatInstanceRepository.deleteAll(removable);
        }

        fic.setTotalSeats(desiredCount);
        flightInstanceCabinRepository.save(fic);
    }

    private List<SeatInstance> buildAvailableSeatInstances(List<Seat> seats, FlightInstanceCabin fic,
                                                           Long flightId, Long flightInstanceId) {
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
