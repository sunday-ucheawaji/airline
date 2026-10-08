package com.sunday.services.service.gateway;

import com.sunday.common_lib.enums.PaymentStatus;

import java.math.BigDecimal;

public record ProviderPaymentStatus(String providerReference, PaymentStatus status, BigDecimal capturedAmount) {
}
