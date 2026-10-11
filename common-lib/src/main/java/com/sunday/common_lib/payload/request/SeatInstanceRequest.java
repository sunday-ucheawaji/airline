package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatInstanceRequest {

    @NotNull(message = ErrorMessageUtil.FLIGHT_ID_MANDATORY)
    private UUID flightId;

    @NotNull(message = ErrorMessageUtil.FLIGHT_INSTANCE_ID_MANDATORY)
    private UUID flightInstanceId;

    @NotNull
    private UUID flightInstanceCabinId;

    @NotNull
    private UUID seatId;

    private SeatAvailabilityStatus status;
    private String mealPreference;
    private BigDecimal fare;
    private UUID flightScheduleId;
}
