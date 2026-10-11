package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatInstanceResponse {
    private UUID id;

    private UUID flightId;
    private UUID seatId;
    private String seatNumber;
    private String seatType;
    private String seatPosition;

    private SeatResponse seat;

    private BigDecimal price;

    private SeatAvailabilityStatus status;

    private UUID flightInstanceId;

    private UUID flightCabinId;
    private CabinClassType flightCabinClassType;

    private String mealPreference;
    private BigDecimal fare;

    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    private Boolean isOccupied;
    private String seatCharacteristics;
}
