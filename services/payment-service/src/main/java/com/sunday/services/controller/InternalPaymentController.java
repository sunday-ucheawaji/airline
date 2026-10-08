package com.sunday.services.controller;

import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.services.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Service-to-service only, not routed by the gateway. */
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/booking/{bookingId}")
    public PaymentDTO getPaymentByBookingId(@PathVariable Long bookingId) {
        return paymentService.getPaymentByBookingId(bookingId);
    }

    @PostMapping("/batch/bookings")
    public Map<Long, PaymentDTO> getPaymentsByBookingIds(@RequestBody List<Long> bookingIds) {
        return paymentService.getPaymentsByBookingIds(bookingIds);
    }
}
