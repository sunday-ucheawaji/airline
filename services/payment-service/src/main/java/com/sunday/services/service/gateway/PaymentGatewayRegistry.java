package com.sunday.services.service.gateway;

import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.exception.PaymentException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Resolves a PaymentGatewayProvider by PaymentGateway. Spring collects every provider bean into the
 *  injected List; adding a 4th provider is one new @Service class, nothing here or in PaymentServiceImpl
 *  changes. */
@Component
@RequiredArgsConstructor
public class PaymentGatewayRegistry {

    private final List<PaymentGatewayProvider> providers;

    private Map<PaymentGateway, PaymentGatewayProvider> byType;

    @PostConstruct
    void index() {
        byType = providers.stream().collect(Collectors.toMap(PaymentGatewayProvider::gatewayType, p -> p));
    }

    public PaymentGatewayProvider resolve(PaymentGateway type) {
        PaymentGatewayProvider provider = byType.get(type);
        if (provider == null) {
            throw new PaymentException(String.format(ErrorMessageUtil.PAYMENT_GATEWAY_NOT_SUPPORTED, type));
        }
        return provider;
    }
}
