package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Atomically holds a specific booking's named seats on one flight instance. All-or-nothing: see seat-service. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldRequest {

    private Long flightInstanceId;
    private CabinClassType cabinClass;
    private List<Long> seatInstanceIds;
    private String bookingReference;
}
