package com.sunday.common_lib.payload.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightMealResponse {
    private UUID id;
    private UUID flightId;
    private MealResponse meal;
    private Boolean available;
    private BigDecimal price;
    private String currency;
    private Integer maxQuantity;
    private String serviceClassRestriction;
    private Integer displayOrder;
    private Boolean complimentary;
    private String notes;
}
