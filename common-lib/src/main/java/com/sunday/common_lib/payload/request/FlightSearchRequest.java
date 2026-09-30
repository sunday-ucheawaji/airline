package com.sunday.common_lib.payload.request;



import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightSearchRequest {


    private Long departureAirportId;
    private Long arrivalAirportId;

    @NotNull(message = ErrorMessageUtil.SEARCH_DEPARTURE_DATE_MANDATORY)
    private LocalDate departureDate;

    @NotNull(message = ErrorMessageUtil.SEARCH_PASSENGERS_MANDATORY)
    @Min(value = 1, message = ErrorMessageUtil.SEARCH_PASSENGERS_MIN)
    private Integer passengers;

    @NotNull(message = ErrorMessageUtil.SEARCH_CABIN_CLASS_MANDATORY)
    private CabinClassType cabinClass;

    // Filter Parameters
    private List<Long> airlines; // Filter by airline ids
    private Double minPrice; // Minimum price filter
    private Double maxPrice; // Maximum price filter
    private String departureTimeRange; // "any", "morning", "afternoon", "evening", "night"
    private String arrivalTimeRange; // "any", "morning", "afternoon", "evening", "night"
    private Integer maxDuration; // Maximum duration in minutes
    private String alliance; // "any", "star", "oneworld", "skyteam"

    // Sorting Parameters
    private String sortBy; // "price", "duration", "departure", "arrival"
    private String sortOrder; // "asc", "desc"
}
