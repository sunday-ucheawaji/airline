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
    /** Seats held, booked or occupied, from seat-service; set on the staff list, null if it couldn't be read. */
    private Long seatsSold;
    private UUID scheduleId;
    /** Free seats in the searched cabin; set by flight search only. */
    private Long seatsLeftInCabin;

    private FlightStatus status;

    private Integer minAdvanceBookingDays;
    private Integer maxAdvanceBookingDays;
    private Boolean isActive;

    private String terminal;
    private String gate;

    private Long version;
    private FareResponse fare;
    private PartyPriceResponse partyPrice;
}
