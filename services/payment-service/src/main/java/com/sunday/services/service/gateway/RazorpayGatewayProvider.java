package com.sunday.services.service.gateway;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;
import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.enums.PaymentStatus;
import com.sunday.common_lib.exception.PaymentException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.model.Payment;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class RazorpayGatewayProvider implements PaymentGatewayProvider {

    @Value("${razorpay.api.key}")
    private String keyId;

    @Value("${razorpay.api.secret}")
    private String keySecret;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    @Value("${razorpay.callback.base-url}")
    private String callbackBaseUrl;

    @Override
    public PaymentGateway gatewayType() {
        return PaymentGateway.RAZORPAY;
    }

    @Override
    public ProviderCheckoutResult createCheckout(Payment payment, UserDTO payer) {
        requireConfigured();
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            JSONObject request = new JSONObject();
            request.put("amount", MoneyUtil.toSmallestUnit(payment.getAmount()));
            request.put("currency", "INR");
            request.put("description", payment.getTransactionId());

            JSONObject customer = new JSONObject();
            customer.put("name", payer.getFirstName() + " " + payer.getLastName());
            customer.put("email", payer.getEmail());
            if (payer.getPhoneNumber() != null) {
                customer.put("contact", payer.getPhoneNumber());
            }
            request.put("customer", customer);

            JSONObject notify = new JSONObject();
            notify.put("email", true);
            notify.put("sms", payer.getPhoneNumber() != null);
            request.put("notify", notify);
            request.put("reminder_enable", true);

            request.put("callback_url", callbackBaseUrl + "/booking-success/" + payment.getBookingId());
            request.put("callback_method", "get");

            JSONObject notes = new JSONObject();
            notes.put("user_id", payer.getId());
            notes.put("payment_id", payment.getId());
            notes.put("booking_id", payment.getBookingId());
            request.put("notes", notes);

            PaymentLink link = razorpay.paymentLink.create(request);
            return new ProviderCheckoutResult(link.get("short_url"), link.get("id"));
        } catch (RazorpayException e) {
            throw new PaymentException(String.format(ErrorMessageUtil.RAZORPAY_PAYMENT_LINK_FAILED, e.getMessage()));
        }
    }

    @Override
    public ProviderPaymentStatus fetchStatus(String providerReference) {
        requireConfigured();
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            com.razorpay.Payment payment = razorpay.payments.fetch(providerReference);
            JSONObject json = payment.toJson();
            return new ProviderPaymentStatus(
                    providerReference,
                    mapStatus(json.optString("status")),
                    MoneyUtil.fromSmallestUnit(json.optLong("amount")));
        } catch (RazorpayException e) {
            throw new PaymentException(String.format(ErrorMessageUtil.RAZORPAY_FETCH_PAYMENT_FAILED, e.getMessage()));
        }
    }

    @Override
    public boolean verifySignature(byte[] rawBody, String signatureHeaderValue) {
        if (webhookSecret == null || webhookSecret.isBlank() || signatureHeaderValue == null) {
            return false;
        }
        try {
            return Utils.verifyWebhookSignature(new String(rawBody, StandardCharsets.UTF_8), signatureHeaderValue, webhookSecret);
        } catch (RazorpayException e) {
            log.warn("Razorpay webhook signature verification error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public ProviderWebhookEvent parseWebhookEvent(byte[] rawBody) {
        try {
            JSONObject root = new JSONObject(new String(rawBody, StandardCharsets.UTF_8));
            String eventType = root.optString("event");
            JSONObject entity = root.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
            String providerReference = entity.optString("id");
            String status = entity.optString("status");
            long amountInPaise = entity.optLong("amount");
            String internalPaymentIdStr = entity.optJSONObject("notes") != null
                    ? entity.getJSONObject("notes").optString("payment_id", null) : null;
            Long internalPaymentId = internalPaymentIdStr == null || internalPaymentIdStr.isBlank()
                    ? null : Long.parseLong(internalPaymentIdStr);
            return new ProviderWebhookEvent(eventType, providerReference, mapStatus(status),
                    MoneyUtil.fromSmallestUnit(amountInPaise), internalPaymentId);
        } catch (Exception e) {
            throw new PaymentException(String.format(ErrorMessageUtil.RAZORPAY_WEBHOOK_PROCESSING_FAILED, e.getMessage()));
        }
    }

    @Override
    public ProviderRefundResult initiateRefund(Payment payment, BigDecimal refundAmount) {
        requireConfigured();
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);
            JSONObject request = new JSONObject();
            request.put("amount", MoneyUtil.toSmallestUnit(refundAmount));
            JSONObject notes = new JSONObject();
            notes.put("payment_id", payment.getId());
            request.put("notes", notes);

            Refund refund = razorpay.payments.refund(payment.getProviderPaymentId(), request);
            return new ProviderRefundResult(refund.get("id"), PaymentStatus.REFUNDED);
        } catch (RazorpayException e) {
            throw new PaymentException(String.format(ErrorMessageUtil.RAZORPAY_REFUND_FAILED, e.getMessage()));
        }
    }

    private PaymentStatus mapStatus(String razorpayStatus) {
        return switch (razorpayStatus) {
            case "captured" -> PaymentStatus.SUCCESS;
            case "failed" -> PaymentStatus.FAILED;
            case "refunded" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }

    private void requireConfigured() {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new PaymentException(ErrorMessageUtil.RAZORPAY_NOT_CONFIGURED);
        }
    }
}
