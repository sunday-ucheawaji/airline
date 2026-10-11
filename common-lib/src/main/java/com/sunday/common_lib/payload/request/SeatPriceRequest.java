package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatPriceRequest {

    @NotEmpty(message = ErrorMessageUtil.SEAT_INSTANCE_IDS_MANDATORY)
    @Size(max = 50, message = ErrorMessageUtil.SEAT_INSTANCE_IDS_MAX_EXCEEDED)
    private List<@NotNull UUID> seatInstanceIds;
}
