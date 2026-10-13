package com.sunday.common_lib.payload.request;

import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequest {

    @NotNull(message = ErrorMessageUtil.PAYMENT_USER_ID_MANDATORY)
    private UUID userId;

    @NotNull(message = ErrorMessageUtil.PAYMENT_BOOKING_ID_MANDATORY)
    private UUID bookingId;

    @NotNull(message = ErrorMessageUtil.PAYMENT_GATEWAY_MANDATORY)
    private PaymentGateway gateway;

    @NotNull(message = ErrorMessageUtil.PAYMENT_AMOUNT_MANDATORY)
    @Positive(message = ErrorMessageUtil.PAYMENT_AMOUNT_POSITIVE)
    private BigDecimal amount;

    @NotBlank(message = ErrorMessageUtil.PAYMENT_CURRENCY_MANDATORY)
    @Pattern(regexp = "^[A-Z]{3}$", message = ErrorMessageUtil.PAYMENT_CURRENCY_INVALID)
    private String currency;

    private String description;

    // Correlates this call back to the caller's own saga/attempt, e.g. the booking's reference.
    // Lets payment-service dedupe a retried Feign call instead of creating a second Payment row.
    private String idempotencyKey;
}
