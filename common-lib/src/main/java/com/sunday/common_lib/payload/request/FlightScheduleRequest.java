package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.RecurrenceType;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightScheduleRequest {

    @NotNull(message = ErrorMessageUtil.FLIGHT_ID_MANDATORY)
    private UUID flightId;

    @NotNull(message = ErrorMessageUtil.AIRCRAFT_ID_MANDATORY)
    private UUID aircraftId;

    private UUID departureAirportId;

    private UUID arrivalAirportId;

    @NotNull(message = ErrorMessageUtil.DEPARTURE_TIME_MANDATORY)
    private LocalTime departureTime;

    @NotNull(message = ErrorMessageUtil.ARRIVAL_TIME_MANDATORY)
    private LocalTime arrivalTime;

    @NotNull(message = ErrorMessageUtil.SCHEDULE_START_DATE_MANDATORY)
    private LocalDate startDate;

    @NotNull(message = ErrorMessageUtil.SCHEDULE_END_DATE_MANDATORY)
    private LocalDate endDate;

    private RecurrenceType recurrenceType;

    private List<DayOfWeek> operatingDays;

    private Boolean isActive;
}
