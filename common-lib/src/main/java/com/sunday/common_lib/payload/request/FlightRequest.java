package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.FlightStatus;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightRequest {

    @NotBlank(message = ErrorMessageUtil.FLIGHT_NUMBER_MANDATORY)
    @Size(max = 10)
    private String flightNumber;

    @NotNull(message = ErrorMessageUtil.AIRLINE_ID_REQUIRED)
    private Long airlineId;

    @NotNull(message = ErrorMessageUtil.DEPARTURE_AIRPORT_ID_MANDATORY)
    private Long departureAirportId;

    @NotNull(message = ErrorMessageUtil.ARRIVAL_AIRPORT_ID_MANDATORY)
    private Long arrivalAirportId;

    private FlightStatus status;
}
