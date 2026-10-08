package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightMealRequest {

    @NotNull(message = "Flight ID is required")
    private Long flightId;

    @NotNull(message = "Meal ID is required")
    private Long mealId;

    @NotNull(message = "Availability status is required")
    private Boolean available;

    @DecimalMin(value = "0.0", message = ErrorMessageUtil.FLIGHT_MEAL_PRICE_NEGATIVE)
    private BigDecimal price;

    @Min(value = 0, message = "Display order cannot be negative")
    private Integer displayOrder;
}
