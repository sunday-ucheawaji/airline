package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightRequest {

    @NotBlank(message = ErrorMessageUtil.FLIGHT_NUMBER_MANDATORY)
    @Size(max = 10)
    private String flightNumber;

    @NotNull(message = ErrorMessageUtil.AIRLINE_ID_REQUIRED)
    private UUID airlineId;

    @NotNull(message = ErrorMessageUtil.DEPARTURE_AIRPORT_ID_MANDATORY)
    private UUID departureAirportId;

    @NotNull(message = ErrorMessageUtil.ARRIVAL_AIRPORT_ID_MANDATORY)
    private UUID arrivalAirportId;

    private FlightStatus status;
}
