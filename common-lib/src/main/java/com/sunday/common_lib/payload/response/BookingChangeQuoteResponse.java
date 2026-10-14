package com.sunday.common_lib.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** What a voluntary change would cost, priced now; nothing is held or charged. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingChangeQuoteResponse {

    private String currency;
    private BigDecimal fares;
    private BigDecimal seatPremiums;
    private BigDecimal ancillaries;
    private BigDecimal meals;
    private BigDecimal changeFees;
    /** Charged for the replacement booking: fares, seats, extras and change fees. */
    private BigDecimal newTotal;
    /** Paid back on the original once the replacement is paid. */
    private BigDecimal originalRefund;
    /** newTotal minus originalRefund; negative when the customer gets money back overall. */
    private BigDecimal netCost;
}
