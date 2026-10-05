package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatMapRequest {

    @NotBlank(message = ErrorMessageUtil.SEAT_MAP_NAME_MANDATORY)
    private String name;

    @NotNull(message = ErrorMessageUtil.SEAT_MAP_TOTAL_ROWS_MANDATORY)
    @Positive
    private Integer totalRows;

    @NotNull(message = ErrorMessageUtil.SEAT_MAP_LEFT_SEATS_PER_ROW_MANDATORY)
    @Positive
    private Integer leftSeatsPerRow;

    @NotNull(message = ErrorMessageUtil.SEAT_MAP_RIGHT_SEATS_PER_ROW_MANDATORY)
    @Positive
    private Integer rightSeatsPerRow;

    private Long cabinClassId;
}
