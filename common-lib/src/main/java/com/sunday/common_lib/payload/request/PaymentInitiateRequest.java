package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequest {

    @NotNull(message = ErrorMessageUtil.PAYMENT_USER_ID_MANDATORY)
    private Long userId;

    @NotNull(message = ErrorMessageUtil.PAYMENT_BOOKING_ID_MANDATORY)
    private Long bookingId;

    @NotNull(message = ErrorMessageUtil.PAYMENT_GATEWAY_MANDATORY)
    private PaymentGateway gateway;

    @NotNull(message = ErrorMessageUtil.PAYMENT_AMOUNT_MANDATORY)
    @Positive(message = ErrorMessageUtil.PAYMENT_AMOUNT_POSITIVE)
    private BigDecimal amount;

    private String description;

    // Correlates this call back to the caller's own saga/attempt, e.g. the booking's reference.
    // Lets payment-service dedupe a retried Feign call instead of creating a second Payment row.
    private String idempotencyKey;
}
