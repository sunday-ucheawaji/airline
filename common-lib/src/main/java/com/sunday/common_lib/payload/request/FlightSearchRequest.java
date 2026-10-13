package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Built manually by {@code FlightSearchController} from individually-required {@code @RequestParam}s,
 * never {@code @Valid}-bound — Bean Validation annotations here would be dead code.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightSearchRequest {

    private UUID departureAirportId;
    private UUID arrivalAirportId;
    private LocalDate departureDate;
    private Integer passengers;
    private CabinClassType cabinClass;

    private List<UUID> airlines;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String departureTimeRange;
    private String arrivalTimeRange;
    private Integer maxDuration;
    private String alliance;

    private String sortBy;
    private String sortOrder;
}
