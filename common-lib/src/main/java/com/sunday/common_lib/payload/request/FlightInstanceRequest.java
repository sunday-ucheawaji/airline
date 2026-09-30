package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstanceRequest {

    @NotNull(message = ErrorMessageUtil.FLIGHT_ID_MANDATORY)
    private Long flightId;

    @NotNull(message = ErrorMessageUtil.AIRCRAFT_ID_MANDATORY)
    private Long aircraftId;

    private Long scheduleId;

    private Long departureAirportId;

    private Long arrivalAirportId;

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
