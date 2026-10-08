package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstanceCabinRequest {

    @NotNull
    private Long flightId;

    @NotNull(message = ErrorMessageUtil.FLIGHT_INSTANCE_ID_MANDATORY)
    private Long flightInstanceId;

    @NotNull
    private Long cabinClassId;

    @NotNull
    @Positive
    private BigDecimal baseFare;

    @NotNull
    private BigDecimal windowSurcharge;

    @NotNull
    private BigDecimal aisleSurcharge;

    @NotNull
    @PositiveOrZero
    private BigDecimal taxesAndFees;

    @NotNull
    @PositiveOrZero
    private BigDecimal airlineFees;

    private BigDecimal currentPrice;
    private Boolean isActive;
}
