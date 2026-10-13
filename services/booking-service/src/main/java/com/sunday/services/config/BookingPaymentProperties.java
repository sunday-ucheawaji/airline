package com.sunday.services.config;

import com.sunday.common_lib.enums.PaymentGateway;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties("booking.payment")
public record BookingPaymentProperties(PaymentGateway defaultGateway, Map<String, PaymentGateway> gateways) {

    public PaymentGateway gatewayFor(String currency) {
        PaymentGateway gateway = gateways != null && currency != null ? gateways.get(currency) : null;
        return gateway != null ? gateway : defaultGateway;
    }
}
