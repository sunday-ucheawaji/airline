package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.FlightStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstanceResponse {

    private UUID id;

    private UUID flightId;
    private String flightNumber;

    private UUID airlineId;
    private String airlineName;
    private String airlineLogo;
    private UUID aircraftId;
    private String aircraftModal;
    private String aircraftCode;
    private AirportResponse departureAirport;
    private AirportResponse arrivalAirport;

    private LocalDateTime departureDateTime;
    private LocalDateTime arrivalDateTime;
    private String formattedDuration;

    private Integer totalSeats;
    private Integer availableSeats;

    private FlightStatus status;

    private Integer minAdvanceBookingDays;
    private Integer maxAdvanceBookingDays;
    private Boolean isActive;

    private String terminal;
    private String gate;

    private Long version;
    private FareResponse fare;
}
