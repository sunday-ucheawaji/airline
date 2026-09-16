package com.sunday.services.clients;

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
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request, Long userId) {
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
    public PaymentDTO getPaymentByBookingId(Long bookingId) {
        return null;
    }

    @Override
    public Map<Long, PaymentDTO> getPaymentsByBookingIds(List<Long> bookingIds) {
        return Collections.emptyMap();
    }
}
