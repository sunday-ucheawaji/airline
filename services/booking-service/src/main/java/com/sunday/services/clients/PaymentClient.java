package com.sunday.services.clients;

import java.util.UUID;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@FeignClient(name = "payment-service", fallbackFactory = PaymentClientFallbackFactory.class)
public interface PaymentClient {

    @PostMapping("/api/payments/initiate")
    PaymentInitiateResponse initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            @RequestHeader("X-User-Id") UUID userId);

    @GetMapping("/internal/payments/booking/{bookingId}")
    PaymentDTO getPaymentByBookingId(@PathVariable UUID bookingId);

    @PostMapping("/internal/payments/{paymentId}/refund")
    PaymentDTO refundPaymentAsSystem(@PathVariable UUID paymentId, @RequestParam(value = "amount", required = false) BigDecimal amount);

    @PostMapping("/internal/payments/batch/bookings")
    Map<UUID, PaymentDTO> getPaymentsByBookingIds(@RequestBody List<UUID> bookingIds);
}
