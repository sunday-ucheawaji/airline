package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.PassengerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** What a whole travelling party pays for one departure, broken down by the fare type each passenger is priced as. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyPriceResponse {

    private String currency;
    private BigDecimal totalPrice;
    private List<PassengerPrice> passengers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassengerPrice {
        private PassengerType passengerType;
        private int count;
        private UUID fareId;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
