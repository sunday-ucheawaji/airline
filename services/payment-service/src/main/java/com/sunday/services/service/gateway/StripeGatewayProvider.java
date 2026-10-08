package com.sunday.services.service.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.enums.PaymentStatus;
import com.sunday.common_lib.exception.PaymentException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.model.Payment;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class StripeGatewayProvider implements PaymentGatewayProvider {

    @Value("${stripe.api.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Value("${stripe.callback.base-url}")
    private String callbackBaseUrl;

    @PostConstruct
    void configureSdk() {
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
        }
    }

    @Override
    public PaymentGateway gatewayType() {
        return PaymentGateway.STRIPE;
    }

    @Override
    public ProviderCheckoutResult createCheckout(Payment payment, UserDTO payer) {
        requireConfigured();
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(callbackBaseUrl + "/booking-success/" + payment.getBookingId())
                    .setCancelUrl(callbackBaseUrl + "/payment-cancelled/" + payment.getId())
                    .setCustomerEmail(payer.getEmail())
                    .putMetadata("payment_id", String.valueOf(payment.getId()))
                    .putMetadata("booking_id", String.valueOf(payment.getBookingId()))
                    .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                            .putMetadata("payment_id", String.valueOf(payment.getId()))
                            .build())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(MoneyUtil.toSmallestUnit(payment.getAmount()))
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Booking " + payment.getBookingId())
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);
            return new ProviderCheckoutResult(session.getUrl(), session.getId());
        } catch (StripeException e) {
            throw new PaymentException(String.format(ErrorMessageUtil.STRIPE_CHECKOUT_FAILED, e.getMessage()));
        }
    }

    @Override
    public ProviderPaymentStatus fetchStatus(String providerReference) {
        requireConfigured();
        try {
            PaymentIntent intent = PaymentIntent.retrieve(providerReference);
            return new ProviderPaymentStatus(providerReference, mapIntentStatus(intent.getStatus()),
                    MoneyUtil.fromSmallestUnit(intent.getAmount()));
        } catch (StripeException e) {
            throw new PaymentException(String.format(ErrorMessageUtil.STRIPE_FETCH_PAYMENT_FAILED, e.getMessage()));
        }
    }

    @Override
    public boolean verifySignature(byte[] rawBody, String signatureHeaderValue) {
        if (webhookSecret == null || webhookSecret.isBlank() || signatureHeaderValue == null) {
            return false;
        }
        try {
            Webhook.constructEvent(new String(rawBody, StandardCharsets.UTF_8), signatureHeaderValue, webhookSecret);
            return true;
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ProviderWebhookEvent parseWebhookEvent(byte[] rawBody) {
        // The interface only passes the raw body here, not the signature header — verifySignature
        // (which does have it) is always called first by the webhook controller and already proved
        // this payload authentic via Webhook.constructEvent. Re-parsing here with plain Jackson (rather
        // than Stripe's typed Event/StripeObject, which needs com.google.gson on the compile classpath —
        // stripe-java only declares it runtime-scoped) is correct and cheap; it is never a substitute
        // for signature verification, only a re-parse after it already passed.
        try {
            JsonNode root = new ObjectMapper().readTree(rawBody);
            String eventType = root.path("type").asText();
            JsonNode object = root.path("data").path("object");
            String objectKind = object.path("object").asText();

            if ("checkout.session".equals(objectKind)) {
                String providerReference = object.path("payment_intent").asText(null);
                Long internalPaymentId = parseLong(object.path("metadata").path("payment_id").asText(null));
                PaymentStatus status = "paid".equalsIgnoreCase(object.path("payment_status").asText())
                        ? PaymentStatus.SUCCESS : PaymentStatus.PENDING;
                return new ProviderWebhookEvent(eventType, providerReference, status,
                        MoneyUtil.fromSmallestUnit(object.path("amount_total").asLong(0)), internalPaymentId);
            }
            if ("payment_intent".equals(objectKind)) {
                Long internalPaymentId = parseLong(object.path("metadata").path("payment_id").asText(null));
                return new ProviderWebhookEvent(eventType, object.path("id").asText(), mapIntentStatus(object.path("status").asText()),
                        MoneyUtil.fromSmallestUnit(object.path("amount").asLong(0)), internalPaymentId);
            }
            throw new PaymentException(String.format(ErrorMessageUtil.STRIPE_WEBHOOK_PROCESSING_FAILED, "unhandled event data type " + objectKind));
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException(String.format(ErrorMessageUtil.STRIPE_WEBHOOK_PROCESSING_FAILED, e.getMessage()));
        }
    }

    @Override
    public ProviderRefundResult initiateRefund(Payment payment, BigDecimal refundAmount) {
        requireConfigured();
        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getProviderPaymentId())
                    .setAmount(MoneyUtil.toSmallestUnit(refundAmount))
                    .build();
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("refund_" + payment.getTransactionId())
                    .build();
            com.stripe.model.Refund refund = com.stripe.model.Refund.create(params, options);
            return new ProviderRefundResult(refund.getId(), PaymentStatus.REFUNDED);
        } catch (StripeException e) {
            throw new PaymentException(String.format(ErrorMessageUtil.STRIPE_REFUND_FAILED, e.getMessage()));
        }
    }

    private Long parseLong(String value) {
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }

    private PaymentStatus mapIntentStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> PaymentStatus.SUCCESS;
            case "canceled" -> PaymentStatus.CANCELLED;
            case "requires_payment_method", "requires_action" -> PaymentStatus.FAILED;
            default -> PaymentStatus.PENDING;
        };
    }

    private void requireConfigured() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new PaymentException(ErrorMessageUtil.STRIPE_NOT_CONFIGURED);
        }
    }
}
