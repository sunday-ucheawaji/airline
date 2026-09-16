package com.sunday.services.clients;

import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class PaymentClientFallbackFactory
        implements FallbackFactory<PaymentClient> {

    @Override
    public PaymentClient create(Throwable cause) {

        log.error(
                "PaymentClient fallback created. Cause: {}",
                cause.getMessage(),
                cause
        );

        return new PaymentClient() {

            @Override
            public PaymentInitiateResponse initiatePayment(
                    PaymentInitiateRequest request,
                    Long userId) {

                log.error(
                        "PaymentClient.initiatePayment FAILED. " +
                        "bookingId={}, userId={}, cause={}",
                        request.getBookingId(),
                        userId,
                        cause.getMessage(),
                        cause
                );

                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Payment service is unavailable"
                );
            }

            @Override
            public PaymentDTO getPaymentByBookingId(Long bookingId) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Payment service is unavailable"
                );
            }

            @Override
            public Map<Long, PaymentDTO> getPaymentsByBookingIds(
                    List<Long> bookingIds) {

                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Payment service is unavailable"
                );
            }
        };
    }
}