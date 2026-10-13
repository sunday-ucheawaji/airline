package com.sunday.services.service;

import com.sunday.common_lib.event.PaymentCompletedEvent;
import com.sunday.common_lib.event.PaymentFailedEvent;

public interface BookingPaymentService {

    void handlePaymentCompleted(PaymentCompletedEvent event);

    void handlePaymentFailed(PaymentFailedEvent event);
}
