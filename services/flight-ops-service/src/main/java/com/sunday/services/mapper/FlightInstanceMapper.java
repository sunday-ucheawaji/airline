package com.sunday.services.mapper;

import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.payload.request.FlightInstanceRequest;
import com.sunday.common_lib.payload.request.FlightInstanceUpdateRequest;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.services.model.Flight;
import com.sunday.services.model.FlightInstance;

public class FlightInstanceMapper {

    public static FlightInstance toEntity(FlightInstanceRequest request, Flight flight) {
        if (request == null) return null;
        return FlightInstance.builder()
                .flight(flight)
                .airlineId(flight.getAirlineId())
                .aircraftId(request.getAircraftId())
                .scheduleId(request.getScheduleId())
                .departureAirportId(request.getDepartureAirportId() != null ?
                        request.getDepartureAirportId() : flight.getDepartureAirportId())
                .arrivalAirportId(request.getArrivalAirportId() != null ?
                        request.getArrivalAirportId() : flight.getArrivalAirportId())
                .departureDateTime(request.getDepartureDateTime())
                .arrivalDateTime(request.getArrivalDateTime())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getAvailableSeats() != null ?
                        request.getAvailableSeats() : request.getTotalSeats())
                .status(request.getStatus() != null ? request.getStatus() : FlightStatus.SCHEDULED)
                .minAdvanceBookingDays(request.getMinAdvanceBookingDays())
                .maxAdvanceBookingDays(request.getMaxAdvanceBookingDays())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .terminal(request.getTerminal())
                .gate(request.getGate())
                .build();
    }

    public static FlightInstanceResponse toResponse(FlightInstance fi,
                                                    AircraftResponse aircraftResponse,
                                                    AirlineResponse airline,
                                                    AirportResponse departureAirport,
                                                    AirportResponse arrivalAirport) {
        if (fi == null) return null;
        return FlightInstanceResponse.builder()
                .id(fi.getId())
                .scheduleId(fi.getScheduleId())
                .flightId(fi.getFlight() != null ? fi.getFlight().getId() : null)
                .flightNumber(fi.getFlight() != null ? fi.getFlight().getFlightNumber() : null)
                .aircraftId(fi.getAircraftId())
                .aircraftModal(aircraftResponse != null ? aircraftResponse.getModel() : null)
                .aircraftCode(aircraftResponse != null ? aircraftResponse.getCode() : null)
                .airlineId(fi.getAirlineId())
                .airlineName(airline != null ? airline.getName() : null)
                .airlineLogo(airline != null ? airline.getLogoUrl() : null)
                .departureAirport(departureAirport)
                .arrivalAirport(arrivalAirport)
                .departureDateTime(fi.getDepartureDateTime())
                .arrivalDateTime(fi.getArrivalDateTime())
                .formattedDuration(fi.getFormattedDuration())
                .totalSeats(fi.getTotalSeats())
                .availableSeats(fi.getAvailableSeats())
                .status(fi.getStatus())
                .minAdvanceBookingDays(fi.getMinAdvanceBookingDays())
                .maxAdvanceBookingDays(fi.getMaxAdvanceBookingDays())
                .isActive(fi.getIsActive())
                .terminal(fi.getTerminal())
                .gate(fi.getGate())
                .version(fi.getVersion())
                .build();
    }

    public static void updateEntity(FlightInstanceUpdateRequest request, FlightInstance existing) {
        existing.setDepartureDateTime(request.getDepartureDateTime());
        existing.setArrivalDateTime(request.getArrivalDateTime());
        existing.setTerminal(blankToNull(request.getTerminal()));
        existing.setGate(blankToNull(request.getGate()));
        existing.setMinAdvanceBookingDays(request.getMinAdvanceBookingDays());
        existing.setMaxAdvanceBookingDays(request.getMaxAdvanceBookingDays());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
