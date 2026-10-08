package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatInstanceResponse {
    private Long id;

    private Long flightId;
    private Long seatId;
    private String seatNumber;
    private String seatType;
    private String seatPosition;

    private SeatResponse seat;

    private BigDecimal price;

    private SeatAvailabilityStatus status;

    private Long flightInstanceId;

    private Long flightCabinId;
    private CabinClassType flightCabinClassType;

    private String mealPreference;
    private BigDecimal fare;

    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    private Boolean isOccupied;
    private String seatCharacteristics;
}
