package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.services.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Service-to-service only, not routed by the gateway. */
@RestController
@RequestMapping("/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @GetMapping("/booking/{bookingId}")
    public PaymentDTO getPaymentByBookingId(@PathVariable UUID bookingId) {
        return paymentService.getPaymentByBookingId(bookingId);
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentDTO refundPayment(@PathVariable UUID paymentId,
                                    @RequestParam(required = false) BigDecimal amount,
                                    @RequestParam(required = false) String reference) {
        return paymentService.refundPaymentAsSystem(paymentId, amount, reference);
    }

    @PostMapping("/batch/bookings")
    public Map<UUID, PaymentDTO> getPaymentsByBookingIds(@RequestBody List<UUID> bookingIds) {
        return paymentService.getPaymentsByBookingIds(bookingIds);
    }
}
