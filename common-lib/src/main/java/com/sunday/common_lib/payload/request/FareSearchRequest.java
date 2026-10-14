package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

    /** The travelling party. Children are priced as {@code CHD}, infants as lap infants ({@code INF}). */
    @Min(value = 1, message = "A search needs at least one adult")
    private int adults = 1;

    @Min(value = 0, message = "Children cannot be negative")
    private int children;

    @Min(value = 0, message = "Infants cannot be negative")
    private int infants;

    public FareSearchRequest(CabinClassType cabinClass, List<FlightDeparture> flights) {
        this(cabinClass, flights, 1, 0, 0);
    }

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
