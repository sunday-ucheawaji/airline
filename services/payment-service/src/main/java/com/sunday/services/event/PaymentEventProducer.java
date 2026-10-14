package com.sunday.services.event;

import com.sunday.common_lib.dto.UserDTO;
import com.sunday.common_lib.event.PaymentCompletedEvent;
import com.sunday.common_lib.event.PaymentFailedEvent;
import com.sunday.common_lib.event.PaymentRefundedEvent;
import com.sunday.services.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendPaymentCompleted(Payment payment) {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .transactionId(payment.getTransactionId())
                .providerPaymentId(payment.getProviderPaymentId())
                .paidAt(payment.getPaidAt())
                .build();

        kafkaTemplate.send("payment.completed", event);
        log.info("Published PaymentCompletedEvent for payment ID: {}, booking ID: {}",
                payment.getId(), payment.getBookingId());
    }

    /** user is best-effort (payment-service's UserClient fails closed to null, not an exception) —
     *  a null contact just means notification-service has nothing to notify, not an error here. */
    public void sendPaymentFailed(Payment payment, UserDTO user) {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .paymentId(payment.getId())
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .transactionId(payment.getTransactionId())
                .failureReason(payment.getFailureReason())
                .failedAt(LocalDateTime.now())
                .contactEmail(user != null ? user.getEmail() : null)
                .contactPhone(user != null ? user.getPhoneNumber() : null)
                .build();

        kafkaTemplate.send("payment.failed", event);
        log.warn("Published PaymentFailedEvent for payment ID: {} - Reason: {}",
                payment.getId(), payment.getFailureReason());
    }

    /** {@code amount} is this refund, not the payment's running refunded total. */
    public void sendPaymentRefunded(Payment payment, UserDTO user, BigDecimal amount, UUID refundId) {
        PaymentRefundedEvent event = PaymentRefundedEvent.builder()
                .paymentId(payment.getId())
                .refundId(refundId)
                .bookingId(payment.getBookingId())
                .userId(payment.getUserId())
                .amount(amount)
                .currency(payment.getCurrency())
                .providerRefundId(payment.getRefundId())
                .refundedAt(LocalDateTime.now())
                .contactEmail(user != null ? user.getEmail() : null)
                .contactPhone(user != null ? user.getPhoneNumber() : null)
                .build();

        kafkaTemplate.send("payment.refunded", event);
        log.info("Published PaymentRefundedEvent for payment ID: {}, booking ID: {}",
                payment.getId(), payment.getBookingId());
    }
}
