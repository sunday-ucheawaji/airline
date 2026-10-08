package com.sunday.services.service;

import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.services.model.BookingSaga;

import java.util.List;

/**
 * Owns every write to the booking_sagas / processed_payment_events tables. The booking_sagas writes
 * each run in their own REQUIRES_NEW transaction (the "claim-first" idiom) so saga progress is
 * durably recorded independently of whatever createBooking's own transaction ultimately does.
 * {@link #claimPaymentEvent} is the exception: it must commit atomically with the caller's own
 * business update, so it deliberately does not use REQUIRES_NEW — see its Javadoc.
 */
public interface BookingSagaService {

    String hashRequest(BookingRequest request, Long userId);

    /** Claims a new saga row for (userId, idempotencyKey), or returns the existing one. A FAILED/CANCELLED
     *  row whose stored requestHash still matches is reset to STARTED so it can be retried. */
    BookingSaga tryStart(Long userId, String idempotencyKey, String requestHash);

    PaymentInitiateResponse readResponse(BookingSaga saga);

    void recordSeatsHeld(Long sagaId, String bookingReference, List<Long> seatInstanceIds);

    void recordBookingCreated(Long sagaId, Long bookingId);

    void recordPaymentInitiated(Long sagaId, PaymentInitiateResponse response);

    void complete(Long bookingId);

    void cancel(Long bookingId);

    void fail(Long sagaId, String error);

    /** True the first time this (paymentId, eventType) pair is seen; false on a redelivered duplicate. */
    boolean claimPaymentEvent(Long paymentId, String eventType);
}
