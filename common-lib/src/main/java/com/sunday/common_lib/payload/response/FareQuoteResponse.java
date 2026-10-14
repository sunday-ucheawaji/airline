package com.sunday.common_lib.payload.response;

import com.sunday.common_lib.enums.PassengerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareQuoteResponse {

    private UUID fareId;
    private PassengerType passengerType;
    private Long version;
    private LocalDate departureDate;
    private String currency;
    private BigDecimal baseFare;
    private BigDecimal taxesAndFees;
    private BigDecimal airlineFees;
    private BigDecimal totalPrice;
    private UUID fareRulesId;
    private UUID baggagePolicyId;
    private Boolean refundable;
    private BigDecimal cancellationFee;
    private Integer refundDeadlineDays;
    private Boolean changeable;
    private BigDecimal changeFee;
    private Integer changeDeadlineHours;
}
