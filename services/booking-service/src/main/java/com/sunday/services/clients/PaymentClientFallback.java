package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class PaymentClientFallback implements PaymentClient {

    @Override
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request, UUID userId) {
        log.error(
                "Payment service fallback triggered. bookingId={}, userId={}",
                request.getBookingId(),
                userId
        );

        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Payment service is currently unavailable"
        );
//        return null;
    }

    @Override
    public PaymentDTO getPaymentByBookingId(UUID bookingId) {
        return null;
    }

    @Override
    public Map<UUID, PaymentDTO> getPaymentsByBookingIds(List<UUID> bookingIds) {
        return Collections.emptyMap();
    }
}
