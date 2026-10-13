package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.SeatType;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** What a customer may see of a seat when choosing one: never who holds or booked it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSeatResponse {

    private UUID seatInstanceId;
    private String seatNumber;
    private Integer row;
    private Character column;
    private SeatType seatType;
    private CabinClassType cabinClass;
    private BigDecimal premiumSurcharge;
    private boolean available;
    private boolean emergencyExit;
    private boolean extraLegroom;
    private boolean bassinet;
    private boolean wheelchairAccessible;
    private boolean powerOutlet;
    private boolean nearLavatory;
}
