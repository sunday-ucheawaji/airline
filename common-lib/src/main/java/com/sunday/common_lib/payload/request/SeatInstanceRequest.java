package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatInstanceRequest {

    @NotNull(message = ErrorMessageUtil.FLIGHT_ID_MANDATORY)
    private Long flightId;

    @NotNull(message = ErrorMessageUtil.FLIGHT_INSTANCE_ID_MANDATORY)
    private Long flightInstanceId;

    @NotNull
    private Long flightInstanceCabinId;

    @NotNull
    private Long seatId;

    private SeatAvailabilityStatus status;
    private String mealPreference;
    private Double fare;
    private Long flightScheduleId;
}
