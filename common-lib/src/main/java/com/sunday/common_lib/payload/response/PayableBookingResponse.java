package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** What payment-service needs to check a payment request against a booking: its owner, its stored total and status. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayableBookingResponse {

    private Long bookingId;
    private Long userId;
    private Long airlineId;
    private BigDecimal totalAmount;
    private BookingStatus status;
}
