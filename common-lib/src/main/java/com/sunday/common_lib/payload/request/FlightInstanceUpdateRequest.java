package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * What staff may change on an existing departure: its times and operational details. The route, aircraft, seat
 * counts and status each have their own rules and endpoints (the aircraft through reassignment, seats in seat-service).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstanceUpdateRequest {

    @NotNull(message = ErrorMessageUtil.DEPARTURE_DATETIME_MANDATORY)
    private LocalDateTime departureDateTime;

    @NotNull(message = ErrorMessageUtil.ARRIVAL_DATETIME_MANDATORY)
    private LocalDateTime arrivalDateTime;

    @Size(max = 20, message = ErrorMessageUtil.DEPARTURE_TERMINAL_TOO_LONG)
    private String terminal;

    @Size(max = 20, message = ErrorMessageUtil.DEPARTURE_GATE_TOO_LONG)
    private String gate;

    @PositiveOrZero(message = ErrorMessageUtil.DEPARTURE_BOOKING_DAYS_NEGATIVE)
    private Integer minAdvanceBookingDays;

    @PositiveOrZero(message = ErrorMessageUtil.DEPARTURE_BOOKING_DAYS_NEGATIVE)
    private Integer maxAdvanceBookingDays;
}
