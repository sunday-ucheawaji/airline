package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.SeatType;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CabinClassRequest {

    @NotNull(message = ErrorMessageUtil.CABIN_CLASS_NAME_MANDATORY)
    private CabinClassType name;

    @NotBlank
    @Size(min = 1, max = 5)
    private String code;

    @Size(max = 500)
    private String description;

    @NotNull
    private UUID aircraftId;

    private Integer displayOrder;
    private Boolean isActive;
    private Boolean isBookable;
    private Integer typicalSeatPitch;
    private Integer typicalSeatWidth;
    private SeatType seatType;
}
