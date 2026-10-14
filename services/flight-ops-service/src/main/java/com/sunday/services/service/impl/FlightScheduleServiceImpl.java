package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.constants.FlightOpsPermissions;
import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.enums.RecurrenceType;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.FlightInstanceRequest;
import com.sunday.common_lib.payload.request.FlightScheduleRequest;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.common_lib.payload.response.FlightScheduleResponse;
import com.sunday.common_lib.payload.response.ScheduleChangeResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.client.LocationClient;
import com.sunday.services.mapper.FlightScheduleMapper;
import com.sunday.services.model.Flight;
import com.sunday.services.model.FlightInstance;
import com.sunday.services.model.FlightSchedule;
import com.sunday.services.repository.FlightInstanceRepository;
import com.sunday.services.repository.FlightRepository;
import com.sunday.services.repository.FlightScheduleRepository;
import com.sunday.services.service.FlightInstanceService;
import com.sunday.services.service.FlightScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A schedule generates one departure per operating day in its date range. Departures are only ever generated in the
 * future, an arrival time earlier than the departure time lands the next day, and a schedule always flies its route's
 * airports. Edits and deletes only touch future departures without sales: a departure with any seat sold or held is
 * someone's booking and stays exactly as it is.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FlightScheduleServiceImpl implements FlightScheduleService {

    private final FlightScheduleRepository flightScheduleRepository;
    private final FlightRepository flightRepository;
    private final FlightInstanceRepository flightInstanceRepository;
    private final FlightInstanceService flightInstanceService;
    private final AirlineIntegrationService airlineIntegrationService;
    private final LocationClient locationClient;
    private final DepartureSales departureSales;

    @Override
    public FlightScheduleResponse createFlightSchedule(UUID userId, FlightScheduleRequest request) {
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, request.getFlightId())));
        airlineIntegrationService.requirePermission(userId, flight.getAirlineId(), FlightOpsPermissions.SCHEDULE_MANAGE);
        requireValidSchedule(request);

        FlightSchedule schedule = FlightScheduleMapper.toEntity(request, flight);
        pinToRoute(schedule, flight);
        resolveOperatingDays(schedule);
        AircraftResponse aircraft = aircraftFor(request, flight, userId);
        FlightSchedule savedSchedule = flightScheduleRepository.save(schedule);

        flightInstanceService.createFlightInstancesInBulk(flight, aircraft,
                departuresFor(savedSchedule, flight, aircraft, Set.of()));

        return getFlightScheduleResponse(savedSchedule);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightScheduleResponse getFlightScheduleById(UUID id, UUID userId) {
        FlightSchedule schedule = findSchedule(id);
        airlineIntegrationService.requirePermission(userId, schedule.getFlight().getAirlineId(), FlightOpsPermissions.SCHEDULE_READ);
        return getFlightScheduleResponse(schedule);
    }

    @Override
    public List<FlightScheduleResponse> getFlightScheduleByAirline(UUID userId, UUID airlineId) {
        airlineIntegrationService.requirePermission(userId, airlineId, FlightOpsPermissions.SCHEDULE_READ);
        List<FlightSchedule> schedules = flightScheduleRepository.findByFlightAirlineId(airlineId);
        if (schedules.isEmpty()) {
            return List.of();
        }

        // One batched airport lookup for the whole list instead of two Feign calls per schedule.
        List<UUID> airportIds = schedules.stream()
                .flatMap(s -> Stream.of(s.getArrivalAirportId(), s.getDepartureAirportId()))
                .distinct().toList();
        Map<UUID, AirportResponse> airports = locationClient.getAirportsByIds(airportIds);

        return schedules.stream()
                .map(s -> FlightScheduleMapper.toResponse(
                        s, airports.get(s.getArrivalAirportId()), airports.get(s.getDepartureAirportId())))
                .collect(Collectors.toList());
    }

    /**
     * Applies the new timetable to every future departure without sales: those are replaced by departures generated
     * from the new pattern (none while the schedule is paused). A departure with sales keeps its date and time, and
     * no new departure is generated on its day.
     */
    @Override
    public ScheduleChangeResponse updateFlightSchedule(UUID id, FlightScheduleRequest request, UUID userId) {
        FlightSchedule existing = findSchedule(id);
        Flight flight = existing.getFlight();
        airlineIntegrationService.requirePermission(userId, flight.getAirlineId(), FlightOpsPermissions.SCHEDULE_MANAGE);
        if (!flight.getId().equals(request.getFlightId())) {
            throw new BadRequestException(ErrorMessageUtil.SCHEDULE_FLIGHT_IMMUTABLE);
        }
        requireValidSchedule(request);
        AircraftResponse aircraft = aircraftFor(request, flight, userId);

        FlightScheduleMapper.updateEntity(request, existing);
        pinToRoute(existing, flight);
        resolveOperatingDays(existing);
        FlightSchedule saved = flightScheduleRepository.save(existing);

        FutureDepartures future = splitFutureDepartures(id);
        flightInstanceService.removeDepartures(future.unsold());
        Set<LocalDate> keptDays = future.withSales().stream()
                .map(fi -> fi.getDepartureDateTime().toLocalDate())
                .collect(Collectors.toSet());
        List<FlightInstanceRequest> generated = Boolean.FALSE.equals(saved.getIsActive())
                ? List.of()
                : departuresFor(saved, flight, aircraft, keptDays);
        flightInstanceService.createFlightInstancesInBulk(flight, aircraft, generated);

        return ScheduleChangeResponse.builder()
                .schedule(getFlightScheduleResponse(saved))
                .departuresRemoved(future.unsold().size())
                .departuresCreated(generated.size())
                .departuresKeptWithSales(future.withSales().size())
                .build();
    }

    /** Future departures without sales go with the schedule; past ones and ones with sales stay, as one-offs. */
    @Override
    public ScheduleChangeResponse deleteFlightSchedule(UUID id, UUID userId) {
        FlightSchedule schedule = findSchedule(id);
        airlineIntegrationService.requirePermission(userId, schedule.getFlight().getAirlineId(), FlightOpsPermissions.SCHEDULE_MANAGE);

        FutureDepartures future = splitFutureDepartures(id);
        flightInstanceService.removeDepartures(future.unsold());
        List<FlightInstance> remaining = flightInstanceRepository.findByScheduleId(id);
        remaining.forEach(fi -> fi.setScheduleId(null));
        flightInstanceRepository.saveAll(remaining);
        flightScheduleRepository.delete(schedule);

        return ScheduleChangeResponse.builder()
                .departuresRemoved(future.unsold().size())
                .departuresKeptWithSales(future.withSales().size())
                .build();
    }

    public FlightScheduleResponse getFlightScheduleResponse(FlightSchedule schedule) {
        AirportResponse arrivalAirport = locationClient.getAirportById(schedule.getArrivalAirportId());
        AirportResponse departureAirport = locationClient.getAirportById(schedule.getDepartureAirportId());
        return FlightScheduleMapper.toResponse(schedule, arrivalAirport, departureAirport);
    }

    // ---------- Helpers ----------

    private record FutureDepartures(List<FlightInstance> unsold, List<FlightInstance> withSales) {}

    private FutureDepartures splitFutureDepartures(UUID scheduleId) {
        List<FlightInstance> future = flightInstanceRepository.findByScheduleIdAndDepartureDateTimeAfter(scheduleId, LocalDateTime.now());
        Map<UUID, Long> sold = departureSales.soldByDeparture(future.stream().map(FlightInstance::getId).toList());
        List<FlightInstance> unsold = new ArrayList<>();
        List<FlightInstance> withSales = new ArrayList<>();
        for (FlightInstance departure : future) {
            (sold.getOrDefault(departure.getId(), 0L) > 0 ? withSales : unsold).add(departure);
        }
        return new FutureDepartures(unsold, withSales);
    }

    /** Every operating day from today (or the start date, if later) to the end date, except the days to skip. */
    private List<FlightInstanceRequest> departuresFor(FlightSchedule schedule, Flight flight, AircraftResponse aircraft,
                                                      Set<LocalDate> skipDays) {
        Set<DayOfWeek> operatingDays = new HashSet<>(schedule.getOperatingDays());
        boolean arrivesNextDay = !schedule.getArrivalTime().isAfter(schedule.getDepartureTime());
        LocalDateTime now = LocalDateTime.now();
        LocalDate first = schedule.getStartDate().isAfter(now.toLocalDate()) ? schedule.getStartDate() : now.toLocalDate();

        List<FlightInstanceRequest> requests = new ArrayList<>();
        for (LocalDate date = first; !date.isAfter(schedule.getEndDate()); date = date.plusDays(1)) {
            LocalDateTime departure = LocalDateTime.of(date, schedule.getDepartureTime());
            if (!operatingDays.contains(date.getDayOfWeek()) || skipDays.contains(date) || !departure.isAfter(now)) {
                continue;
            }
            LocalDateTime arrival = LocalDateTime.of(arrivesNextDay ? date.plusDays(1) : date, schedule.getArrivalTime());
            requests.add(FlightInstanceRequest.builder()
                    .scheduleId(schedule.getId())
                    .flightId(flight.getId())
                    .aircraftId(aircraft.getId())
                    .departureAirportId(flight.getDepartureAirportId())
                    .arrivalAirportId(flight.getArrivalAirportId())
                    .departureDateTime(departure)
                    .arrivalDateTime(arrival)
                    .totalSeats(aircraft.getTotalSeats())
                    .status(FlightStatus.SCHEDULED)
                    .build());
        }
        return requests;
    }

    private AircraftResponse aircraftFor(FlightScheduleRequest request, Flight flight, UUID userId) {
        AircraftResponse aircraft = airlineIntegrationService.getAircraftById(request.getAircraftId(), userId);
        FlightInstanceServiceImpl.requireSameAirline(aircraft, flight.getAirlineId());
        return aircraft;
    }

    private static void requireValidSchedule(FlightScheduleRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException(ErrorMessageUtil.SCHEDULE_END_DATE_BEFORE_START_DATE);
        }
        if (request.getEndDate().isBefore(LocalDate.now())) {
            throw new BadRequestException(ErrorMessageUtil.SCHEDULE_END_DATE_IN_PAST);
        }
        if (request.getDepartureTime().equals(request.getArrivalTime())) {
            throw new BadRequestException(ErrorMessageUtil.SCHEDULE_TIMES_EQUAL);
        }
    }

    private static void pinToRoute(FlightSchedule schedule, Flight flight) {
        schedule.setDepartureAirportId(flight.getDepartureAirportId());
        schedule.setArrivalAirportId(flight.getArrivalAirportId());
    }

    /** A daily schedule runs every day; any other needs its days listed. */
    private static void resolveOperatingDays(FlightSchedule schedule) {
        if (schedule.getRecurrenceType() == RecurrenceType.DAILY) {
            schedule.setOperatingDays(new ArrayList<>(List.of(DayOfWeek.values())));
        } else if (schedule.getOperatingDays() == null || schedule.getOperatingDays().isEmpty()) {
            throw new BadRequestException(ErrorMessageUtil.SCHEDULE_OPERATING_DAYS_MANDATORY);
        }
    }

    private FlightSchedule findSchedule(UUID id) {
        return flightScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_SCHEDULE_NOT_FOUND_BY_ID, id)));
    }
}
