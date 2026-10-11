package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.SeatType;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatRequest {

    @NotBlank(message = ErrorMessageUtil.SEAT_NUMBER_MANDATORY)
    @Size(min = 2, max = 10)
    private String seatNumber;

    @NotNull(message = ErrorMessageUtil.SEAT_ROW_MANDATORY)
    @Positive
    private Integer seatRow;

    private Character columnLetter;

    @NotNull(message = ErrorMessageUtil.SEAT_TYPE_MANDATORY)
    private SeatType seatType;

    @NotNull(message = ErrorMessageUtil.SEAT_MAP_ID_MANDATORY)
    private UUID seatMapId;

    private UUID cabinClassId;

    private Boolean isAvailable;
    private Boolean isBlocked;
    private Boolean isEmergencyExit;
    private Boolean isActive;

    private BigDecimal basePrice;
    private BigDecimal premiumSurcharge;

    private Boolean hasExtraLegroom;
    private Boolean hasBassinet;
    private Boolean isNearLavatory;
    private Boolean isNearGalley;
    private Boolean hasPowerOutlet;
    private Boolean hasTvScreen;
    private Boolean isWheelchairAccessible;
    private Boolean hasExtraWidth;

    private Integer seatPitch;
    private Integer seatWidth;
    private Integer reclineAngle;
}
