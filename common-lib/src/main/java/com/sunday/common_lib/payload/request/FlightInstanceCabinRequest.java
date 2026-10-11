package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightInstanceCabinRequest {

    @NotNull
    private UUID flightId;

    @NotNull(message = ErrorMessageUtil.FLIGHT_INSTANCE_ID_MANDATORY)
    private UUID flightInstanceId;

    @NotNull
    private UUID cabinClassId;

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
