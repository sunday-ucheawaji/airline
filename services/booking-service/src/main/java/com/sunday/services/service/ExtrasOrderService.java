package com.sunday.services.service;

import com.sunday.common_lib.event.PaymentCompletedEvent;
import com.sunday.common_lib.payload.request.ExtrasOrderRequest;
import com.sunday.common_lib.payload.response.ExtrasOrderResponse;
import com.sunday.services.model.Booking;
import com.sunday.services.model.ExtrasOrder;

import java.util.UUID;

public interface ExtrasOrderService {

    /**
     * Prices the extras on the booking's flight and cabin, within the per-passenger limits counting what the booking
     * already has, and starts their payment. Owner or BOOKING_MANAGE; the booking must be confirmed and not departed.
     * With an Idempotency-Key, a retry returns the same order (and its payment) instead of making another.
     */
    ExtrasOrderResponse orderExtras(UUID bookingId, ExtrasOrderRequest request, UUID userId, String idempotencyKey);

    /** The order's payment completed: confirm it, or refund the payment if the order or its booking is no longer live. */
    void handlePaid(ExtrasOrder order, PaymentCompletedEvent event);

    void handleFailed(ExtrasOrder order);

    /**
     * The booking is cancelled or exchanged: its open orders are closed, and confirmed ones are refunded in full when
     * {@code refund} is true. Runs inside the caller's transaction, refunds first.
     */
    void closeOrders(Booking booking, boolean refund);
}
