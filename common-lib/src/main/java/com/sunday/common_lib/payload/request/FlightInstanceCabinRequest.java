package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;

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
    private Double baseFare;

    @NotNull
    private Double windowSurcharge;

    @NotNull
    private Double aisleSurcharge;

    @NotNull
    @PositiveOrZero
    private Double taxesAndFees;

    @NotNull
    @PositiveOrZero
    private Double airlineFees;

    private Double currentPrice;
    private Boolean isActive;
}
