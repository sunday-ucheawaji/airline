package com.sunday.services.controller;

import com.sunday.common_lib.exception.PaymentException;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            @RequestHeader("X-User-Id") Long userId) throws PaymentException {


            PaymentInitiateResponse response = paymentService
                    .initiatePayment(request);
            return ResponseEntity.ok(response);


    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false)
            String signature)
            throws PaymentException {

            log.info("Received RazorPay payment verification request");
            PaymentDTO payment = paymentService.verifyPayment(payload, signature);
            return ResponseEntity.ok(payment);

    }

//    @PostMapping("/webhook/razorpay")
//    public ResponseEntity<Void> handleRazorpayWebhook(
//            @RequestBody String payload,
//            @RequestHeader(value = "X-Razorpay-Signature", required = false)
//            String signature) throws PaymentException {
//
//        log.info("========== RAZORPAY WEBHOOK ==========");
//        log.info("Signature: {}", signature);
//        log.info("Payload: {}", payload);
//        log.info("======================================");
//
//        paymentService.handleRazorpayWebhook(payload, signature);
//
//        return ResponseEntity.ok().build();
//    }

    @PostMapping("/batch/bookings")
    public ResponseEntity<Map<Long, PaymentDTO>> getPaymentsByBookingIds(@RequestBody List<Long> bookingIds) {
        return ResponseEntity.ok(paymentService
                .getPaymentsByBookingIds(bookingIds));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentDTO>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestHeader("X-User-Id") Long userId) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<PaymentDTO> payments = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(payments);
    }





    private record ErrorResponse(String message) {}
}
