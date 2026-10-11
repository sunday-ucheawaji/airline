package com.sunday.common_lib.payload.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareRulesResponse {

    private UUID id;
    private String ruleName;
    private UUID fareId;
    private UUID airlineId;
    private Boolean isRefundable;
    private BigDecimal changeFee;
    private BigDecimal cancellationFee;
    private Integer refundDeadlineDays;
    private Integer changeDeadlineHours;
    private Boolean isChangeable;
    private Instant createdAt;
    private Instant updatedAt;
}
