package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import feign.FeignException;
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
                    UUID userId) {

                log.error(
                        "PaymentClient.initiatePayment FAILED. " +
                        "bookingId={}, userId={}, cause={}",
                        request.getBookingId(),
                        userId,
                        cause.getMessage(),
                        cause
                );

                // A 4xx from payment-service (not the booking's owner, already paid, amount
                // mismatch, gateway validation failure) is a legitimate rejection — pass it on
                // with its real status and message instead of masking it as "unavailable".
                if (cause instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500) {
                    throw new ResponseStatusException(
                            HttpStatus.valueOf(fe.status()),
                            FeignFailureUtil.extractMessage(fe, "Payment could not be initiated"));
                }

                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Payment service is unavailable"
                );
            }

            @Override
            public PaymentDTO getPaymentByBookingId(UUID bookingId) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Payment service is unavailable"
                );
            }

            @Override
            public Map<UUID, PaymentDTO> getPaymentsByBookingIds(
                    List<UUID> bookingIds) {

                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Payment service is unavailable"
                );
            }
        };
    }
}