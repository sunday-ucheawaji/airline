package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.services.event.publisher.BookingEventProducer;
import com.sunday.services.model.Booking;
import com.sunday.services.model.BookingSaga;
import com.sunday.services.model.SagaStatus;
import com.sunday.services.repository.BookingRepository;
import com.sunday.services.repository.BookingSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Backstop for createBooking sagas that never reach a terminal state: the seat-hold TTL sweeper in
 * seat-service releases the seats on its own, but nothing else ever moved the Booking row itself (or
 * a saga that crashed before a Booking row even existed) out of PENDING/in-flight. This is the actual
 * correctness guarantee for the "payment webhook never arrives" case, with no dependency on any Kafka
 * event firing — same rationale as SeatHoldExpirySweeper.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSagaRecoverySweeper {

    private final BookingSagaRepository bookingSagaRepository;
    private final BookingRepository bookingRepository;
    private final BookingEventProducer bookingEventProducer;

    @Value("${booking.saga.stale-after-minutes:10}")
    private long staleAfterMinutes;

    @Value("${booking.saga.payment-timeout-minutes:20}")
    private long paymentTimeoutMinutes;

    @Scheduled(fixedDelayString = "${booking.saga.sweep-interval:5m}")
    @Transactional
    public void sweep() {
        sweepNeverCreated();
        sweepNeverConfirmed();
    }

    private void sweepNeverCreated() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(staleAfterMinutes);
        List<BookingSaga> stuck = bookingSagaRepository.findByStatusInAndUpdatedAtBefore(
                List.of(SagaStatus.STARTED, SagaStatus.SEATS_HELD), cutoff);
        for (BookingSaga saga : stuck) {
            bookingEventProducer.sendBookingCancelled(saga.getBookingId(), saga.getBookingReference(), saga.getSeatInstanceIds());
            saga.setStatus(SagaStatus.FAILED);
            saga.setLastError("Abandoned before a booking was created; swept after " + staleAfterMinutes + "m");
        }
        if (!stuck.isEmpty()) {
            bookingSagaRepository.saveAll(stuck);
            log.info("Swept {} abandoned saga(s) that never reached a created booking", stuck.size());
        }
    }

    private void sweepNeverConfirmed() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);
        List<BookingSaga> stuck = bookingSagaRepository.findByStatusInAndUpdatedAtBefore(
                List.of(SagaStatus.BOOKING_CREATED, SagaStatus.PAYMENT_INITIATED), cutoff);
        for (BookingSaga saga : stuck) {
            Booking booking = saga.getBookingId() != null ? bookingRepository.findById(saga.getBookingId()).orElse(null) : null;
            if (booking != null && booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
            }
            bookingEventProducer.sendBookingCancelled(saga.getBookingId(), saga.getBookingReference(), saga.getSeatInstanceIds());
            saga.setStatus(SagaStatus.CANCELLED);
            saga.setLastError("Payment never confirmed or failed; swept after " + paymentTimeoutMinutes + "m");
        }
        if (!stuck.isEmpty()) {
            bookingSagaRepository.saveAll(stuck);
            log.info("Swept {} booking(s) whose payment never resolved", stuck.size());
        }
    }
}
