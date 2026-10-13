package com.sunday.services.event.listener;

import com.sunday.common_lib.event.PaymentCompletedEvent;
import com.sunday.common_lib.event.PaymentFailedEvent;
import com.sunday.services.service.BookingPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final BookingPaymentService bookingPaymentService;

    @KafkaListener(topics = "payment.completed", groupId = "booking-service-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        bookingPaymentService.handlePaymentCompleted(event);
    }

    @KafkaListener(topics = "payment.failed", groupId = "booking-service-group")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        bookingPaymentService.handlePaymentFailed(event);
    }
}
