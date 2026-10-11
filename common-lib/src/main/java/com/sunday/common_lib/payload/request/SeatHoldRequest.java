package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Atomically holds a specific booking's named seats on one flight instance. All-or-nothing: see seat-service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldRequest {

    private UUID flightInstanceId;
    private CabinClassType cabinClass;
    private List<UUID> seatInstanceIds;
    private String bookingReference;
}
