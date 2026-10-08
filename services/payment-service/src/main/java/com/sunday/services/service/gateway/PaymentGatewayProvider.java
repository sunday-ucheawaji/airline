package com.sunday.services.service.gateway;

import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.services.model.Payment;

import java.math.BigDecimal;

/**
 * One implementation per payment provider (Razorpay/Stripe/Paystack). PaymentServiceImpl never branches
 * on PaymentGateway itself — it resolves the right provider through PaymentGatewayRegistry and calls
 * this interface, so adding a new provider means one new class, zero changes to PaymentServiceImpl.
 */
public interface PaymentGatewayProvider {

    PaymentGateway gatewayType();

    /** Creates a hosted checkout/payment-link. The provider reference returned must be stored as
     *  Payment.providerPaymentId — it's what fetchStatus/initiateRefund key off later. */
    ProviderCheckoutResult createCheckout(Payment payment, UserDTO payer);

    /** Authoritative server-to-server status lookup by the provider's own reference (not the webhook
     *  body) — used to cross-check a webhook before trusting it. */
    ProviderPaymentStatus fetchStatus(String providerReference);

    /** Verifies the signature over the RAW request body bytes, captured before any JSON parsing —
     *  a re-serialized body is not byte-identical to what was signed. */
    boolean verifySignature(byte[] rawBody, String signatureHeaderValue);

    /** Parses an already-signature-verified raw body into one provider-neutral event shape. */
    ProviderWebhookEvent parseWebhookEvent(byte[] rawBody);

    /** Initiates a refund at the provider for a previously successful payment. */
    ProviderRefundResult initiateRefund(Payment payment, BigDecimal refundAmount);
}
