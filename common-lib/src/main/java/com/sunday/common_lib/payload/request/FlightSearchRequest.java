package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightSearchRequest {

    private UUID departureAirportId;
    private UUID arrivalAirportId;

    @NotNull(message = ErrorMessageUtil.SEARCH_DEPARTURE_DATE_MANDATORY)
    private LocalDate departureDate;

    @NotNull(message = ErrorMessageUtil.SEARCH_PASSENGERS_MANDATORY)
    @Min(value = 1, message = ErrorMessageUtil.SEARCH_PASSENGERS_MIN)
    private Integer passengers;

    @NotNull(message = ErrorMessageUtil.SEARCH_CABIN_CLASS_MANDATORY)
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
