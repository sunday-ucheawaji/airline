package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstanceRequest {

    @NotNull(message = ErrorMessageUtil.FLIGHT_ID_MANDATORY)
    private UUID flightId;

    @NotNull(message = ErrorMessageUtil.AIRCRAFT_ID_MANDATORY)
    private UUID aircraftId;

    private UUID scheduleId;

    private UUID departureAirportId;

    private UUID arrivalAirportId;

    @NotNull(message = ErrorMessageUtil.DEPARTURE_DATETIME_MANDATORY)
    private LocalDateTime departureDateTime;

    @NotNull(message = ErrorMessageUtil.ARRIVAL_DATETIME_MANDATORY)
    private LocalDateTime arrivalDateTime;

    @NotNull(message = ErrorMessageUtil.TOTAL_SEATS_MANDATORY)
    @Positive
    private Integer totalSeats;

    @PositiveOrZero
    private Integer availableSeats;

    private FlightStatus status;

    private Integer minAdvanceBookingDays;
    private Integer maxAdvanceBookingDays;
    private Boolean isActive;

    private String terminal;
    private String gate;
}
