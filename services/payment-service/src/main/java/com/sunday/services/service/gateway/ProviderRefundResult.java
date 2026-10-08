package com.sunday.services.service.gateway;

import com.sunday.common_lib.enums.PaymentStatus;

public record ProviderRefundResult(String providerRefundId, PaymentStatus status) {
}
