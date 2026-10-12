package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightMealRequest {

    @NotNull(message = "Flight ID is required")
    private UUID flightId;

    @NotNull(message = "Meal ID is required")
    private UUID mealId;

    @NotNull(message = "Availability status is required")
    private Boolean available;

    @DecimalMin(value = "0.0", message = ErrorMessageUtil.FLIGHT_MEAL_PRICE_NEGATIVE)
    private BigDecimal price;

    @Min(value = 0, message = ErrorMessageUtil.DISPLAY_ORDER_NEGATIVE)
    private Integer displayOrder;
}
