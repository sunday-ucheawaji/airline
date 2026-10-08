package com.sunday.common_lib.payload.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightMealResponse {
    private Long id;
    private Long flightId;
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
