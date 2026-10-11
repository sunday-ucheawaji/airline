package com.sunday.common_lib.payload.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MealResponse {
    private UUID id;
    private String code;
    private String name;
    private String description;
    private String mealType;
    private String dietaryRestriction;
    private String ingredients;
    private String allergens;
    private String nutritionalInfo;
    private String imageUrl;
    private BigDecimal price;
    private String currency;
    private Boolean available;
    private Boolean requiresAdvanceBooking;
    private Integer advanceBookingHours;
    private Integer displayOrder;
    private UUID airlineId;
    private Instant createdAt;
    private Instant updatedAt;
}
