package com.sunday.services.controller;

import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.services.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * One route per provider, each gateway-public (permitAll at api-gateway's SecurityConfig — a real
 * provider webhook call carries no platform JWT) and secured instead by that provider's own signature
 * verification inside PaymentServiceImpl.handleWebhook. Separate routes because each provider's
 * verification needs a specific header checked against a specific secret — guessing which provider
 * sent an unsigned/malformed request would be a weaker posture than making the route the source of truth.
 */
@RestController
@RequestMapping("/api/payments/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/razorpay")
    public ResponseEntity<PaymentDTO> razorpay(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        return ResponseEntity.ok(paymentService.handleWebhook(PaymentGateway.RAZORPAY, rawBody, signature));
    }

    @PostMapping("/stripe")
    public ResponseEntity<PaymentDTO> stripe(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        return ResponseEntity.ok(paymentService.handleWebhook(PaymentGateway.STRIPE, rawBody, signature));
    }

    @PostMapping("/paystack")
    public ResponseEntity<PaymentDTO> paystack(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {
        return ResponseEntity.ok(paymentService.handleWebhook(PaymentGateway.PAYSTACK, rawBody, signature));
    }
}
