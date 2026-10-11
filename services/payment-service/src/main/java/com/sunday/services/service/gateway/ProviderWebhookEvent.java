package com.sunday.services.service.gateway;

import com.sunday.common_lib.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

/** internalPaymentId is the provider-neutral form of "the payment_id we stashed in the provider's own
 *  metadata/notes field at checkout-creation time" — every adapter must set it there and read it back here. */
public record ProviderWebhookEvent(
        String eventType,
        String providerReference,
        PaymentStatus status,
        BigDecimal amount,
        UUID internalPaymentId) {
}
