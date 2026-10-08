package com.sunday.services.service.gateway;

import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.enums.PaymentStatus;
import com.sunday.common_lib.exception.PaymentException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Paystack has no official Java SDK, so this is a plain REST client against api.paystack.co (bearer
 * auth with the secret key). Webhook signing uses HMAC-SHA512 of the raw body with that SAME secret
 * key — Paystack has no separate webhook-specific secret, unlike Razorpay/Stripe.
 */
@Slf4j
@Service
public class PaystackGatewayProvider implements PaymentGatewayProvider {

    private static final String BASE_URL = "https://api.paystack.co";

    @Value("${paystack.api.secret-key}")
    private String secretKey;

    @Value("${paystack.callback.base-url}")
    private String callbackBaseUrl;

    private final RestClient restClient = RestClient.create(BASE_URL);

    @Override
    public PaymentGateway gatewayType() {
        return PaymentGateway.PAYSTACK;
    }

    @Override
    public ProviderCheckoutResult createCheckout(Payment payment, UserDTO payer) {
        requireConfigured();
        try {
            String reference = "PS_" + payment.getId() + "_" + UUID.randomUUID().toString().substring(0, 8);
            Map<String, Object> body = Map.of(
                    "email", payer.getEmail(),
                    "amount", MoneyUtil.toSmallestUnit(payment.getAmount()),
                    "reference", reference,
                    "callback_url", callbackBaseUrl + "/booking-success/" + payment.getBookingId(),
                    "metadata", Map.of("payment_id", String.valueOf(payment.getId()), "booking_id", String.valueOf(payment.getBookingId())));

            String json = restClient.post()
                    .uri("/transaction/initialize")
                    .headers(h -> h.setBearerAuth(secretKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JSONObject response = json != null ? new JSONObject(json) : null;
            if (response == null || !response.optBoolean("status")) {
                throw new PaymentException(String.format(ErrorMessageUtil.PAYSTACK_CHECKOUT_FAILED, "no response"));
            }
            JSONObject data = response.getJSONObject("data");
            return new ProviderCheckoutResult(data.getString("authorization_url"), data.getString("reference"));
        } catch (Exception e) {
            throw new PaymentException(String.format(ErrorMessageUtil.PAYSTACK_CHECKOUT_FAILED, e.getMessage()));
        }
    }

    @Override
    public ProviderPaymentStatus fetchStatus(String providerReference) {
        requireConfigured();
        try {
            String json = restClient.get()
                    .uri("/transaction/verify/{reference}", providerReference)
                    .headers(h -> h.setBearerAuth(secretKey))
                    .retrieve()
                    .body(String.class);
            JSONObject data = new JSONObject(json).getJSONObject("data");
            return new ProviderPaymentStatus(providerReference, mapStatus(data.optString("status")),
                    MoneyUtil.fromSmallestUnit(data.optLong("amount")));
        } catch (Exception e) {
            throw new PaymentException(String.format(ErrorMessageUtil.PAYSTACK_FETCH_PAYMENT_FAILED, e.getMessage()));
        }
    }

    @Override
    public boolean verifySignature(byte[] rawBody, String signatureHeaderValue) {
        if (secretKey == null || secretKey.isBlank() || signatureHeaderValue == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            String expectedHex = HexFormat.of().formatHex(mac.doFinal(rawBody));
            return MessageDigest.isEqual(
                    expectedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeaderValue.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("Paystack webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ProviderWebhookEvent parseWebhookEvent(byte[] rawBody) {
        try {
            JSONObject root = new JSONObject(new String(rawBody, StandardCharsets.UTF_8));
            String eventType = root.optString("event");
            JSONObject data = root.getJSONObject("data");
            String reference = data.optString("reference");
            String status = data.optString("status");
            long amountInKobo = data.optLong("amount");
            JSONObject metadata = data.optJSONObject("metadata");
            String internalPaymentIdStr = metadata != null ? metadata.optString("payment_id", null) : null;
            Long internalPaymentId = internalPaymentIdStr == null || internalPaymentIdStr.isBlank()
                    ? null : Long.parseLong(internalPaymentIdStr);
            return new ProviderWebhookEvent(eventType, reference, mapStatus(status),
                    MoneyUtil.fromSmallestUnit(amountInKobo), internalPaymentId);
        } catch (Exception e) {
            throw new PaymentException(String.format(ErrorMessageUtil.PAYSTACK_WEBHOOK_PROCESSING_FAILED, e.getMessage()));
        }
    }

    @Override
    public ProviderRefundResult initiateRefund(Payment payment, BigDecimal refundAmount) {
        requireConfigured();
        try {
            Map<String, Object> body = Map.of(
                    "transaction", payment.getProviderPaymentId(),
                    "amount", MoneyUtil.toSmallestUnit(refundAmount));
            String json = restClient.post()
                    .uri("/refund")
                    .headers(h -> h.setBearerAuth(secretKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JSONObject response = new JSONObject(json);
            if (!response.optBoolean("status")) {
                throw new PaymentException(String.format(ErrorMessageUtil.PAYSTACK_REFUND_FAILED, response.optString("message")));
            }
            JSONObject data = response.getJSONObject("data");
            return new ProviderRefundResult(String.valueOf(data.opt("id")), PaymentStatus.REFUNDED);
        } catch (Exception e) {
            throw new PaymentException(String.format(ErrorMessageUtil.PAYSTACK_REFUND_FAILED, e.getMessage()));
        }
    }

    private PaymentStatus mapStatus(String paystackStatus) {
        return switch (paystackStatus) {
            case "success" -> PaymentStatus.SUCCESS;
            case "failed", "abandoned" -> PaymentStatus.FAILED;
            case "reversed" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }

    private void requireConfigured() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new PaymentException(ErrorMessageUtil.PAYSTACK_NOT_CONFIGURED);
        }
    }
}
