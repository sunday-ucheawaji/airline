package com.sunday.services.config;

import com.sunday.common_lib.enums.PaymentGateway;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * The currencies the platform accepts and the gateway that takes each. There is no fallback gateway and no currency
 * conversion: a currency that isn't listed is refused before anything is held or charged.
 */
@ConfigurationProperties("booking.payment")
public record BookingPaymentProperties(Map<String, PaymentGateway> gateways) {

    /** The gateway for this currency, or null if payments in it are not accepted. */
    public PaymentGateway gatewayFor(String currency) {
        return gateways != null && currency != null ? gateways.get(currency) : null;
    }
}
