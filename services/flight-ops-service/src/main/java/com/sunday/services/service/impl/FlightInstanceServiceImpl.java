package com.sunday.services.service.impl;

import java.util.UUID;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.EnumSet;
import com.sunday.common_lib.payload.request.FlightInstanceUpdateRequest;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.event.FlightInstancesDeletedEvent;
import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.constants.FlightOpsPermissions;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.event.FlightInstanceAircraftChangedEvent;
import com.sunday.common_lib.event.FlightInstanceCreatedEvent;
import com.sunday.common_lib.event.FlightInstancesBulkCreatedEvent;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.request.FlightInstanceRequest;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.common_lib.payload.response.CabinClassResponse;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.common_lib.util.CacheUtil;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.client.AirlineClient;
import com.sunday.services.client.LocationClient;
import com.sunday.services.client.SeatClient;
import com.sunday.services.event.FlightInstanceEventProducer;
import com.sunday.services.mapper.FlightInstanceMapper;
import com.sunday.services.model.Flight;
import com.sunday.services.model.FlightInstance;
import com.sunday.services.repository.FlightInstanceRepository;
import com.sunday.services.repository.FlightRepository;
import com.sunday.services.service.FlightInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class FlightInstanceServiceImpl implements FlightInstanceService {

    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightRepository flightRepository;
    private final AirlineClient airlineClient;
    private final SeatClient seatClient;
    private final FlightInstanceEventProducer flightInstanceEventProducer;
    private final AirlineIntegrationService airlineIntegrationService;
    private final LocationClient locationClient;
    private final DepartureSales departureSales;

    /** Where a departure may go next. A cancelled or completed departure is final. */
    private static final Map<FlightStatus, Set<FlightStatus>> NEXT_STATUSES = Map.of(
            FlightStatus.SCHEDULED, EnumSet.of(FlightStatus.DELAYED, FlightStatus.BOARDING, FlightStatus.CANCELLED),
            FlightStatus.DELAYED, EnumSet.of(FlightStatus.SCHEDULED, FlightStatus.BOARDING, FlightStatus.CANCELLED),
            FlightStatus.BOARDING, EnumSet.of(FlightStatus.DEPARTED, FlightStatus.DELAYED, FlightStatus.CANCELLED),
            FlightStatus.DEPARTED, EnumSet.of(FlightStatus.IN_AIR),
            FlightStatus.IN_AIR, EnumSet.of(FlightStatus.LANDED, FlightStatus.DIVERTED),
            FlightStatus.DIVERTED, EnumSet.of(FlightStatus.LANDED),
            FlightStatus.LANDED, EnumSet.of(FlightStatus.ARRIVED),
            FlightStatus.ARRIVED, EnumSet.of(FlightStatus.COMPLETED));

    private static final Set<FlightStatus> EDITABLE_STATUSES = EnumSet.of(FlightStatus.SCHEDULED, FlightStatus.DELAYED);

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheUtil.FLIGHT_INSTANCES, allEntries = true)
    public FlightInstanceResponse createFlightInstanceWithCabins(
            UUID userId, FlightInstanceRequest request) {

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, request.getFlightId())));
        airlineIntegrationService.requirePermission(userId, flight.getAirlineId(), FlightOpsPermissions.INSTANCE_MANAGE);

        requireValidTimes(request.getDepartureDateTime(), request.getArrivalDateTime());
        requireBookingWindow(request.getMinAdvanceBookingDays(), request.getMaxAdvanceBookingDays());
        AircraftResponse aircraft = airlineIntegrationService.getAircraftById(request.getAircraftId(), userId);
        requireSameAirline(aircraft, flight.getAirlineId());

        request.setScheduleId(null);
        request.setStatus(FlightStatus.SCHEDULED);
        FlightInstance flightInstance = flightInstanceRepository.save(buildInstanceEntity(request, flight, aircraft));
        publishInstanceCreated(flightInstance, flight.getId());

        return getFlightInstance(flightInstance);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CacheUtil.FLIGHT_INSTANCES, allEntries = true)
    public List<FlightInstance> createFlightInstancesInBulk(Flight flight, AircraftResponse aircraft, List<FlightInstanceRequest> requests) {
        if (requests.isEmpty()) return List.of();

        List<FlightInstance> saved = flightInstanceRepository.saveAll(
                requests.stream().map(request -> buildInstanceEntity(request, flight, aircraft)).toList());

        // One event for the whole batch — every instance here shares the same aircraft, so
        // seat-service can resolve its cabin configuration once and materialize all of them in
        // one pass, instead of redoing that lookup per instance.
        flightInstanceEventProducer.sendFlightInstancesBulkCreated(
                FlightInstancesBulkCreatedEvent.builder()
                        .flightId(flight.getId())
                        .aircraftId(aircraft.getId())
                        .flightInstanceIds(saved.stream().map(FlightInstance::getId).toList())
                        .build()
        );
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheUtil.FLIGHT_INSTANCES, key = "#id")
    public FlightInstanceResponse getFlightInstanceById(UUID id) {
        FlightInstance fi = flightInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_NOT_FOUND_BY_ID, id)));


        return getFlightInstance(fi);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlightInstanceResponse> getByAirlineId(UUID userId,
                                                       UUID airlineId,
                                                       UUID departureAirportId,
                                                       UUID arrivalAirportId,
                                                       UUID flightId,
                                                       LocalDate onDate,
                                                       Pageable pageable) {
        airlineIntegrationService.requirePermission(userId, airlineId, FlightOpsPermissions.INSTANCE_READ);
        LocalDateTime start = onDate != null ? onDate.atStartOfDay() : null;
        LocalDateTime end   = onDate != null ? onDate.plusDays(1).atStartOfDay() : null;

        Page<FlightInstance> page = flightInstanceRepository.findByAirlineIdWithFilters(
                airlineId, departureAirportId, arrivalAirportId, flightId, start, end, pageable);
        Map<UUID, FlightInstanceResponse> responses = toResponses(page.getContent());
        Map<UUID, Long> sold = departureSales.soldByDepartureForDisplay(responses.keySet());
        return page.map(fi -> {
            FlightInstanceResponse response = responses.get(fi.getId());
            response.setSeatsSold(sold.get(fi.getId()));
            return response;
        });
    }

    @Override
    @CacheEvict(cacheNames = CacheUtil.FLIGHT_INSTANCES, key = "#id")
    public FlightInstanceResponse updateFlightInstance(UUID id, FlightInstanceUpdateRequest request, UUID userId) {
        FlightInstance existing = findInstance(id);
        airlineIntegrationService.requirePermission(userId, existing.getAirlineId(), FlightOpsPermissions.INSTANCE_MANAGE);
        if (!EDITABLE_STATUSES.contains(existing.getStatus())) {
            throw new ConflictException(String.format(ErrorMessageUtil.DEPARTURE_NOT_EDITABLE, describe(existing.getStatus())));
        }
        requireValidTimes(request.getDepartureDateTime(), request.getArrivalDateTime());
        requireBookingWindow(request.getMinAdvanceBookingDays(), request.getMaxAdvanceBookingDays());
        FlightInstanceMapper.updateEntity(request, existing);
        return getFlightInstance(flightInstanceRepository.save(existing));
    }

    @Override
    @CacheEvict(cacheNames = CacheUtil.FLIGHT_INSTANCES, key = "#id")
    public FlightInstanceResponse changeStatus(UUID id, FlightStatus status, UUID userId) {
        FlightInstance existing = findInstance(id);
        airlineIntegrationService.requirePermission(userId, existing.getAirlineId(), FlightOpsPermissions.INSTANCE_MANAGE);
        if (!NEXT_STATUSES.getOrDefault(existing.getStatus(), Set.of()).contains(status)) {
            throw new ConflictException(String.format(ErrorMessageUtil.DEPARTURE_STATUS_TRANSITION,
                    describe(existing.getStatus()), describe(status)));
        }
        if (status == FlightStatus.CANCELLED) {
            long sold = departureSales.soldFor(id);
            if (sold > 0) {
                throw new ConflictException(String.format(ErrorMessageUtil.DEPARTURE_CANCEL_HAS_SALES, sold));
            }
        }
        existing.setStatus(status);
        return getFlightInstance(flightInstanceRepository.save(existing));
    }

    @Override
    @CacheEvict(cacheNames = CacheUtil.FLIGHT_INSTANCES, key = "#id")
    public void deleteFlightInstance(UUID id, UUID userId) {
        FlightInstance fi = findInstance(id);
        airlineIntegrationService.requirePermission(userId, fi.getAirlineId(), FlightOpsPermissions.INSTANCE_MANAGE);
        long sold = departureSales.soldFor(id);
        if (sold > 0) {
            throw new ConflictException(String.format(ErrorMessageUtil.DEPARTURE_HAS_SALES, sold));
        }
        removeDepartures(List.of(fi));
    }

    @Override
    @CacheEvict(cacheNames = CacheUtil.FLIGHT_INSTANCES, allEntries = true)
    public void removeDepartures(List<FlightInstance> departures) {
        if (departures.isEmpty()) {
            return;
        }
        flightInstanceRepository.deleteAll(departures);
        flightInstanceRepository.flush();
        flightInstanceEventProducer.sendFlightInstancesDeleted(FlightInstancesDeletedEvent.builder()
                .flightInstanceIds(departures.stream().map(FlightInstance::getId).toList())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, FlightInstanceResponse> getFlightInstancesByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return toResponses(flightInstanceRepository.findAllByIdInWithFlight(ids));
    }

    @Override
    @CacheEvict(cacheNames = CacheUtil.FLIGHT_INSTANCES, key = "#id")
    public FlightInstanceResponse reassignAircraft(UUID id, UUID newAircraftId, UUID userId) {
        FlightInstance instance = flightInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, instance.getAirlineId(), FlightOpsPermissions.INSTANCE_AIRCRAFT_ASSIGN);

        AircraftResponse newAircraft = airlineIntegrationService.getAircraftById(newAircraftId, userId);
        requireSameAirline(newAircraft, instance.getAirlineId());

        List<CabinSeatStatusResponse> soldSummary = seatClient.getSeatStatusSummary(id);
        if (soldSummary == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.SEAT_SERVICE_UNAVAILABLE);
        }

        List<CabinClassResponse> newCabinClasses = seatClient.getCabinClassesByAircraftId(newAircraftId);
        Map<CabinClassType, Integer> newCapacityByCabin = new HashMap<>();
        for (CabinClassResponse cabinClass : newCabinClasses) {
            int capacity = cabinClass.getSeatMap() != null && cabinClass.getSeatMap().getTotalSeats() != null
                    ? cabinClass.getSeatMap().getTotalSeats() : 0;
            newCapacityByCabin.put(CabinClassType.valueOf(cabinClass.getName()), capacity);
        }

        long totalNonAvailable = 0;
        for (CabinSeatStatusResponse cabinStatus : soldSummary) {
            totalNonAvailable += cabinStatus.getNonAvailableSeatCount();
            if (cabinStatus.getNonAvailableSeatCount() > 0) {
                Integer newCapacity = newCapacityByCabin.get(cabinStatus.getCabinClass());
                if (newCapacity == null || newCapacity < cabinStatus.getNonAvailableSeatCount()) {
                    throw new ConflictException(String.format(
                            ErrorMessageUtil.AIRCRAFT_REASSIGN_CAPACITY_EXCEEDED,
                            cabinStatus.getNonAvailableSeatCount(), cabinStatus.getCabinClass(),
                            newCapacity == null ? "none of that cabin class" : newCapacity + " seat(s) of that cabin class"));
                }
            }
        }

        UUID oldAircraftId = instance.getAircraftId();
        instance.setAircraftId(newAircraftId);
        instance.setTotalSeats(newAircraft.getTotalSeats());
        instance.setAvailableSeats(newAircraft.getTotalSeats() - (int) totalNonAvailable);
        FlightInstance saved = flightInstanceRepository.save(instance);

        // Enqueues onto the outbox (same transaction as the save above) rather than publishing
        // to Kafka directly — see OutboxEventPublisher.
        flightInstanceEventProducer.sendFlightInstanceAircraftChanged(
                FlightInstanceAircraftChangedEvent.builder()
                        .flightInstanceId(saved.getId())
                        .flightId(saved.getFlight().getId())
                        .oldAircraftId(oldAircraftId)
                        .newAircraftId(newAircraftId)
                        .build()
        );

        return getFlightInstance(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** One round trip per external service for the whole list, not one per departure. */
    private Map<UUID, FlightInstanceResponse> toResponses(List<FlightInstance> instances) {
        if (instances.isEmpty()) return Map.of();
        List<UUID> distinctAirlineIds = instances.stream().map(FlightInstance::getAirlineId).distinct().toList();
        List<UUID> distinctAircraftIds = instances.stream().map(FlightInstance::getAircraftId).distinct().toList();
        List<UUID> distinctAirportIds = instances.stream()
                .flatMap(fi -> Stream.of(fi.getDepartureAirportId(), fi.getArrivalAirportId()))
                .distinct().toList();
        Map<UUID, AirlineResponse> airlines = airlineClient.getAirlinesByIdsInternal(distinctAirlineIds);
        Map<UUID, AircraftResponse> aircrafts = airlineIntegrationService.getAircraftsByIdsInternal(distinctAircraftIds);
        Map<UUID, AirportResponse> airports = locationClient.getAirportsByIds(distinctAirportIds);

        Map<UUID, FlightInstanceResponse> result = new LinkedHashMap<>();
        for (FlightInstance fi : instances) {
            result.put(fi.getId(), FlightInstanceMapper.toResponse(
                    fi,
                    aircrafts.get(fi.getAircraftId()),
                    airlines.get(fi.getAirlineId()),
                    airports.get(fi.getDepartureAirportId()),
                    airports.get(fi.getArrivalAirportId())));
        }
        return result;
    }

    private FlightInstance findInstance(UUID id) {
        return flightInstanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_NOT_FOUND_BY_ID, id)));
    }

    private static void requireValidTimes(LocalDateTime departure, LocalDateTime arrival) {
        if (!arrival.isAfter(departure)) {
            throw new BadRequestException(ErrorMessageUtil.DEPARTURE_ARRIVAL_NOT_AFTER_DEPARTURE);
        }
        if (!departure.isAfter(LocalDateTime.now())) {
            throw new BadRequestException(ErrorMessageUtil.DEPARTURE_IN_PAST);
        }
    }

    private static void requireBookingWindow(Integer minDays, Integer maxDays) {
        if (minDays != null && maxDays != null && minDays > maxDays) {
            throw new BadRequestException(ErrorMessageUtil.DEPARTURE_BOOKING_WINDOW);
        }
    }

    static void requireSameAirline(AircraftResponse aircraft, UUID airlineId) {
        if (aircraft.getAirlineId() != null && !aircraft.getAirlineId().equals(airlineId)) {
            throw new BadRequestException(ErrorMessageUtil.AIRCRAFT_NOT_IN_AIRLINE);
        }
    }

    private static String describe(FlightStatus status) {
        return status.name().toLowerCase().replace('_', ' ');
    }

    /** A departure always flies its route's airports; they are never taken from the request. */
    private FlightInstance buildInstanceEntity(FlightInstanceRequest request, Flight flight, AircraftResponse aircraft) {
        FlightInstance instance = FlightInstanceMapper.toEntity(request, flight);
        instance.setDepartureAirportId(flight.getDepartureAirportId());
        instance.setArrivalAirportId(flight.getArrivalAirportId());
        instance.setTotalSeats(aircraft.getTotalSeats());
        instance.setAvailableSeats(aircraft.getTotalSeats());
        return instance;
    }

    // seat-service consumes this to materialize per-cabin seat inventory for the new instance
    private void publishInstanceCreated(FlightInstance flightInstance, UUID flightId) {
        flightInstanceEventProducer.sendFlightInstanceCreated(
                FlightInstanceCreatedEvent.builder()
                        .flightInstanceId(flightInstance.getId())
                        .aircraftId(flightInstance.getAircraftId())
                        .flightId(flightId)
                        .build()
        );
    }

    private FlightInstanceResponse getFlightInstance(FlightInstance fi) {
        AirlineResponse airline          = airlineClient.getAirlineByIdInternal(fi.getAirlineId());
        AirportResponse departureAirport = locationClient.getAirportById(fi.getDepartureAirportId());
        AirportResponse arrivalAirport   = locationClient.getAirportById(fi.getArrivalAirportId());
        AircraftResponse aircraftResponse = airlineIntegrationService.getAircraftByIdInternal(fi.getAircraftId());
        return FlightInstanceMapper.toResponse(fi,
                aircraftResponse, airline,
                departureAirport, arrivalAirport);
    }
}
