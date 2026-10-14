package com.sunday.service.listener;

import com.sunday.common_lib.event.BookingCancelledEvent;
import com.sunday.common_lib.event.BookingConfirmedEvent;
import com.sunday.common_lib.event.ExtrasConfirmedEvent;
import com.sunday.common_lib.event.PaymentFailedEvent;
import com.sunday.common_lib.event.PaymentRefundedEvent;
import com.sunday.service.content.EmailContentBuilder;
import com.sunday.service.content.SmsContentBuilder;
import com.sunday.service.model.NotificationChannel;
import com.sunday.service.model.NotificationEventType;
import com.sunday.service.service.NotificationDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

/**
 * One listener method per event type, all sharing the default container factory (generic Object,
 * type-header-resolved — see KafkaConsumerConfig). Each dispatches email and SMS in parallel
 * (NotificationDispatchService itself handles idempotency, retry-ability and resilience per channel)
 * and lets any failure propagate — unlike the listener this replaces, which caught and swallowed
 * every send exception, silently defeating the container's retry/DLT config.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationDispatchService dispatchService;
    private final EmailContentBuilder emailContent;
    private final SmsContentBuilder smsContent;
    private final ExecutorService notificationDispatchExecutor;

    @KafkaListener(topics = "booking.confirmed", groupId = "notification-service-group")
    public void handleBookingConfirmed(@Payload BookingConfirmedEvent event) {
        log.info("Received BookingConfirmedEvent: bookingRef={}", event.getBookingReference());
        dispatchBoth(NotificationEventType.BOOKING_CONFIRMED, event.getBookingReference(),
                event.getContactEmail(), emailContent.subjectForBookingConfirmed(event), () -> emailContent.bodyForBookingConfirmed(event),
                event.getContactPhone(), () -> smsContent.bodyForBookingConfirmed(event));
    }

    @KafkaListener(topics = "booking.cancelled", groupId = "notification-service-group")
    public void handleBookingCancelled(@Payload BookingCancelledEvent event) {
        log.info("Received BookingCancelledEvent: bookingRef={}", event.getBookingReference());
        dispatchBoth(NotificationEventType.BOOKING_CANCELLED, event.getBookingReference(),
                event.getContactEmail(), emailContent.subjectForBookingCancelled(event), () -> emailContent.bodyForBookingCancelled(event),
                event.getContactPhone(), () -> smsContent.bodyForBookingCancelled(event));
    }

    @KafkaListener(topics = "payment.failed", groupId = "notification-service-group")
    public void handlePaymentFailed(@Payload PaymentFailedEvent event) {
        log.info("Received PaymentFailedEvent: paymentId={}", event.getPaymentId());
        dispatchBoth(NotificationEventType.PAYMENT_FAILED, String.valueOf(event.getPaymentId()),
                event.getContactEmail(), emailContent.subjectForPaymentFailed(event), () -> emailContent.bodyForPaymentFailed(event),
                event.getContactPhone(), () -> smsContent.bodyForPaymentFailed(event));
    }

    @KafkaListener(topics = "payment.refunded", groupId = "notification-service-group")
    public void handlePaymentRefunded(@Payload PaymentRefundedEvent event) {
        log.info("Received PaymentRefundedEvent: paymentId={}", event.getPaymentId());
        // One notification per refund: a payment refunded in parts sends one for each part.
        String reference = String.valueOf(event.getRefundId() != null ? event.getRefundId() : event.getPaymentId());
        dispatchBoth(NotificationEventType.PAYMENT_REFUNDED, reference,
                event.getContactEmail(), emailContent.subjectForPaymentRefunded(event), () -> emailContent.bodyForPaymentRefunded(event),
                event.getContactPhone(), () -> smsContent.bodyForPaymentRefunded(event));
    }

    @KafkaListener(topics = ExtrasConfirmedEvent.TOPIC, groupId = "notification-service-group")
    public void handleExtrasConfirmed(@Payload ExtrasConfirmedEvent event) {
        log.info("Received ExtrasConfirmedEvent: orderId={}", event.getOrderId());
        dispatchBoth(NotificationEventType.EXTRAS_CONFIRMED, String.valueOf(event.getOrderId()),
                event.getContactEmail(), emailContent.subjectForExtrasConfirmed(event), () -> emailContent.bodyForExtrasConfirmed(event),
                event.getContactPhone(), () -> smsContent.bodyForExtrasConfirmed(event));
    }

    private void dispatchBoth(NotificationEventType eventType, String reference,
                               String contactEmail, String subject, java.util.function.Supplier<String> emailBody,
                               String contactPhone, java.util.function.Supplier<String> smsBody) {
        CompletableFuture<Void> emailFuture = CompletableFuture.runAsync(() ->
                dispatchService.dispatch(eventType, reference, NotificationChannel.EMAIL, contactEmail, subject, emailBody.get()),
                notificationDispatchExecutor);
        CompletableFuture<Void> smsFuture = CompletableFuture.runAsync(() ->
                dispatchService.dispatch(eventType, reference, NotificationChannel.SMS, contactPhone, null, smsBody.get()),
                notificationDispatchExecutor);

        try {
            CompletableFuture.allOf(emailFuture, smsFuture).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }
}
