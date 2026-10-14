package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Moves a confirmed booking to another departure of the same airline. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingChangeRequest {

    @NotNull(message = ErrorMessageUtil.BOOKING_CHANGE_FLIGHT_INSTANCE_MANDATORY)
    private UUID flightInstanceId;

    /** The adult fare on the new flight. */
    @NotNull(message = ErrorMessageUtil.BOOKING_CHANGE_FARE_MANDATORY)
    private UUID fareId;

    /** Defaults to the booking's cabin. */
    private CabinClassType cabinClass;

    /** A seat on the new departure for every passenger except lap infants. */
    @Valid
    private List<SeatAssignment> seats;

    /** Extras on the new flight. The original's extras belong to its flight and are part of its refund. */
    private List<UUID> ancillaryIds;
    private List<UUID> mealIds;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatAssignment {

        @NotNull(message = ErrorMessageUtil.SEAT_ASSIGNMENT_PASSENGER_MANDATORY)
        private UUID passengerId;

        @NotNull(message = ErrorMessageUtil.SEAT_ASSIGNMENT_SEAT_MANDATORY)
        private UUID seatInstanceId;
    }
}
