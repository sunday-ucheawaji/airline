package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FareSearchRequest {

    @NotNull(message = "Cabin class is required")
    private CabinClassType cabinClass;

    @NotEmpty(message = "At least one flight departure is required")
    @Valid
    private List<FlightDeparture> flights;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlightDeparture {

        @NotNull(message = "Flight ID is required")
        private UUID flightId;

        @NotNull(message = "Departure date is required")
        private LocalDate departureDate;
    }
}
