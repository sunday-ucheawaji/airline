package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.request.PaymentRefundRequest;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.services.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /** The payer is the authenticated caller (X-User-Id), never the body. The service checks they own the booking. */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        request.setUserId(userId);
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    /** Deprecated alias for an existing Razorpay dashboard webhook URL — point new configs at
     *  POST /api/payments/webhooks/razorpay instead. Removed after one release. */
    @Deprecated
    @PostMapping("/verify")
    public ResponseEntity<PaymentDTO> verifyLegacy(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        log.warn("Deprecated /api/payments/verify called — point the Razorpay dashboard webhook at /api/payments/webhooks/razorpay");
        return ResponseEntity.ok(paymentService.handleWebhook(PaymentGateway.RAZORPAY, rawBody, signature));
    }

    /** Owner of the payment (customer receipt lookup), or PAYMENT_READ on its airline. */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentDTO> getPaymentForBooking(
            @PathVariable UUID bookingId,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(paymentService.getPaymentForBooking(bookingId, userId));
    }

    /** Airline-scoped list, gated on PAYMENT_READ for that airline. Replaces the old unscoped GET /. */
    @GetMapping("/airline/{airlineId}")
    public ResponseEntity<Page<PaymentDTO>> getPaymentsForAirline(
            @PathVariable UUID airlineId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(paymentService.getPaymentsForAirline(airlineId, userId, pageable));
    }

    /** Platform support read across airlines — PAYMENT_SUPPORT_READ is enforced at the gateway. */
    @GetMapping("/support/booking/{bookingId}")
    public ResponseEntity<PaymentDTO> getPaymentForSupport(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentForSupport(bookingId));
    }

    /** Never owner-eligible: a customer must never execute their own refund. */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentDTO> refundPayment(
            @PathVariable UUID paymentId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody(required = false) PaymentRefundRequest request) {
        BigDecimal amount = request != null ? request.getAmount() : null;
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, userId, amount));
    }
}
