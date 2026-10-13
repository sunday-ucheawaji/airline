package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Turns a paid booking's held seats into booked ones, replacing any seat that was lost in the meantime. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatConfirmRequest {

    private UUID flightInstanceId;
    private CabinClassType cabinClass;
    private String bookingReference;
    private List<UUID> seatInstanceIds;
}
