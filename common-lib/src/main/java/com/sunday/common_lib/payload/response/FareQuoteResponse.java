package com.sunday.common_lib.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareQuoteResponse {

    private Long fareId;
    private Long version;
    private LocalDate departureDate;
    private String currency;
    private Double baseFare;
    private Double taxesAndFees;
    private Double airlineFees;
    private Double totalPrice;
    private Long fareRulesId;
    private Long baggagePolicyId;
}
