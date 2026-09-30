package com.sunday.services.service.impl;

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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AirlineIntegrationService airlineIntegrationService;
    private final LocationClient locationClient;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "flightInstances", allEntries = true)
    public FlightInstanceResponse createFlightInstanceWithCabins(
            Long userId, FlightInstanceRequest request) {

        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, request.getFlightId())));
        airlineIntegrationService.requirePermission(userId, flight.getAirlineId(), FlightOpsPermissions.INSTANCE_MANAGE);

        AircraftResponse aircraft = airlineIntegrationService.getAircraftById(request.getAircraftId(), userId);

        FlightInstance flightInstance = flightInstanceRepository.save(buildInstanceEntity(request, flight, aircraft));
        publishInstanceCreated(flightInstance, flight.getId());

        return getFlightInstance(flightInstance);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "flightInstances", allEntries = true)
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
    @Cacheable(cacheNames = "flightInstances", key = "#id")
    public FlightInstanceResponse getFlightInstanceById(Long id) {
        FlightInstance fi = flightInstanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_NOT_FOUND_BY_ID, id)));


        return getFlightInstance(fi);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlightInstanceResponse> getByAirlineId(Long userId,
                                                       Long airlineId,
                                                       Long departureAirportId,
                                                       Long arrivalAirportId,
                                                       Long flightId,
                                                       LocalDate onDate,
                                                       Pageable pageable) {
        airlineIntegrationService.requirePermission(userId, airlineId, FlightOpsPermissions.INSTANCE_READ);
        LocalDateTime start = onDate != null ? onDate.atStartOfDay() : null;
        LocalDateTime end   = onDate != null ? onDate.plusDays(1).atStartOfDay() : null;

        return flightInstanceRepository.findByAirlineIdWithFilters(
                airlineId, departureAirportId, arrivalAirportId, flightId, start, end, pageable
        ).map(this::getFlightInstance);
    }

    @Override
    @CacheEvict(cacheNames = "flightInstances", key = "#id")
    public FlightInstanceResponse updateFlightInstance(Long id, FlightInstanceRequest request, Long userId) {
        FlightInstance existing = flightInstanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, existing.getAirlineId(), FlightOpsPermissions.INSTANCE_MANAGE);
        FlightInstanceMapper.updateEntity(request, existing);
        return getFlightInstance(flightInstanceRepository.save(existing));
    }

    @Override
    @CacheEvict(cacheNames = "flightInstances", key = "#id")
    public void deleteFlightInstance(Long id, Long userId) {
        FlightInstance fi = flightInstanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, fi.getAirlineId(), FlightOpsPermissions.INSTANCE_MANAGE);
        flightInstanceRepository.delete(fi);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, FlightInstanceResponse> getFlightInstancesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        List<FlightInstance> instances = flightInstanceRepository.findAllByIdInWithFlight(ids);

        // One round trip per external service instead of one per distinct id.
        List<Long> distinctAirlineIds = instances.stream().map(FlightInstance::getAirlineId).distinct().toList();
        List<Long> distinctAircraftIds = instances.stream().map(FlightInstance::getAircraftId).distinct().toList();
        List<Long> distinctAirportIds = instances.stream()
                .flatMap(fi -> Stream.of(fi.getDepartureAirportId(), fi.getArrivalAirportId()))
                .distinct().toList();
        Map<Long, AirlineResponse> airlines = airlineClient.getAirlinesByIdsInternal(distinctAirlineIds);
        Map<Long, AircraftResponse> aircrafts = airlineIntegrationService.getAircraftsByIdsInternal(distinctAircraftIds);
        Map<Long, AirportResponse> airports = locationClient.getAirportsByIds(distinctAirportIds);

        Map<Long, FlightInstanceResponse> result = new HashMap<>();
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

    @Override
    @CacheEvict(cacheNames = "flightInstances", key = "#id")
    public FlightInstanceResponse reassignAircraft(Long id, Long newAircraftId, Long userId) {
        FlightInstance instance = flightInstanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, instance.getAirlineId(), FlightOpsPermissions.INSTANCE_AIRCRAFT_ASSIGN);

        AircraftResponse newAircraft = airlineIntegrationService.getAircraftById(newAircraftId, userId);

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

        Long oldAircraftId = instance.getAircraftId();
        instance.setAircraftId(newAircraftId);
        instance.setTotalSeats(newAircraft.getTotalSeats());
        instance.setAvailableSeats(newAircraft.getTotalSeats() - (int) totalNonAvailable);
        FlightInstance saved = flightInstanceRepository.save(instance);

        applicationEventPublisher.publishEvent(
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

    private FlightInstance buildInstanceEntity(FlightInstanceRequest request, Flight flight, AircraftResponse aircraft) {
        FlightInstance instance = FlightInstanceMapper.toEntity(request, flight);
        instance.setTotalSeats(aircraft.getTotalSeats());
        instance.setAvailableSeats(aircraft.getTotalSeats());
        return instance;
    }

    // seat-service consumes this to materialize per-cabin seat inventory for the new instance
    private void publishInstanceCreated(FlightInstance flightInstance, Long flightId) {
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
