package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightCabinAncillaryRequest {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotNull(message = "Cabin Class ID is required")
    private Long cabinClassId;

    @NotNull(message = "Ancillary ID is required")
    private Long ancillaryId;

    @NotNull(message = "Availability status is required")
    private Boolean available;

    private Integer maxQuantity;

    @NotNull(message = ErrorMessageUtil.PRICE_MANDATORY)
    @PositiveOrZero(message = ErrorMessageUtil.PRICE_MUST_BE_ZERO_OR_POSITIVE)
    private BigDecimal price;

    private String currency;

    @NotNull(message = "Included in fare status is required")
    private Boolean includedInFare;
}
