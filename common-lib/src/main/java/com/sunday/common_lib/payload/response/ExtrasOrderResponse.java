package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtrasOrderResponse {

    private UUID id;
    private UUID bookingId;
    private BookingStatus status;
    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;
    private UUID paymentId;
    private List<UUID> ancillaryIds;
    private List<UUID> mealIds;
    private LocalDateTime createdAt;
    /** Set when the order was just placed and has something to pay. */
    private PaymentInitiateResponse payment;
}
