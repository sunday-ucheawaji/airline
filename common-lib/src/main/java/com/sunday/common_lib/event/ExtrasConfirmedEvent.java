package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/** Extras bought after a booking's purchase were paid for (or were free) and are confirmed. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtrasConfirmedEvent {

    public static final String TOPIC = "extras.confirmed";

    private UUID orderId;
    private UUID bookingId;
    private String bookingReference;
    private String currency;
    private BigDecimal totalAmount;
    private int ancillaryCount;
    private int mealCount;
    private String contactEmail;
    private String contactPhone;
}
