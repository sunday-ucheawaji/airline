package com.sunday.services.service;

import com.sunday.common_lib.enums.PaymentGateway;
import com.sunday.common_lib.payload.request.PaymentInitiateRequest;
import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PaymentService {

    PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request);

    /** Shared entry point for all three providers' webhook routes — gateway/eventType dispatch happens
     *  entirely through PaymentGatewayProvider, not here. */
    PaymentDTO handleWebhook(PaymentGateway gateway, byte[] rawBody, String signatureHeaderValue);

    /** Owner of the payment, or PAYMENT_READ on its airline (e.g. a receipt lookup). */
    PaymentDTO getPaymentForBooking(Long bookingId, Long userId);

    /** Airline-scoped list, gated on PAYMENT_READ for that airline. */
    Page<PaymentDTO> getPaymentsForAirline(Long airlineId, Long userId, Pageable pageable);

    /** Platform support read across airlines — permission is checked at the gateway, not here. */
    PaymentDTO getPaymentForSupport(Long bookingId);

    /** Gated on PAYMENT_REFUND_EXECUTE for the payment's airline. amount null means a full refund. */
    PaymentDTO refundPayment(Long paymentId, Long userId, BigDecimal amount);

    PaymentDTO getPaymentByBookingId(Long bookingId);

    Map<Long, PaymentDTO> getPaymentsByBookingIds(List<Long> bookingIds);
}
