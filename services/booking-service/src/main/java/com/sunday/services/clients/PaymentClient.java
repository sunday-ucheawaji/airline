package com.sunday.services.clients;

import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "payment-service",
//        fallback = PaymentClientFallback.class,
        fallbackFactory = PaymentClientFallbackFactory.class

)
public interface PaymentClient {

    @PostMapping("/api/payments/initiate")
    PaymentInitiateResponse initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            @RequestHeader("X-User-Id") Long userId);

    @GetMapping("/api/payments/booking/{bookingId}")
    PaymentDTO getPaymentByBookingId(@PathVariable Long bookingId);

    @PostMapping("/api/payments/batch/bookings")
    Map<Long, PaymentDTO> getPaymentsByBookingIds(@RequestBody List<Long> bookingIds);
}
