package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.event.publisher.BookingEventProducer;
import com.sunday.services.model.Booking;
import com.sunday.services.model.BookingSaga;
import com.sunday.services.model.SagaStatus;
import com.sunday.services.repository.BookingRepository;
import com.sunday.services.repository.ExtrasOrderRepository;
import com.sunday.services.repository.TripRepository;
import com.sunday.services.repository.BookingSagaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSagaRecoverySweeper {

    private final BookingSagaRepository bookingSagaRepository;
    private final BookingRepository bookingRepository;
    private final BookingEventProducer bookingEventProducer;
    private final TripRepository tripRepository;
    private final ExtrasOrderRepository extrasOrderRepository;

    @Value("${booking.saga.stale-after-minutes:10}")
    private long staleAfterMinutes;

    @Value("${booking.saga.payment-timeout-minutes:20}")
    private long paymentTimeoutMinutes;

    @Scheduled(fixedDelayString = "${booking.saga.sweep-interval:5m}")
    @Transactional
    public void sweep() {
        sweepNeverCreated();
        sweepNeverConfirmed();
        int expired = extrasOrderRepository.cancelUnpaidBefore(LocalDateTime.now().minusMinutes(paymentTimeoutMinutes));
        if (expired > 0) {
            log.info("Cancelled {} extras order(s) never paid", expired);
        }
    }

    private void sweepNeverCreated() {
        List<BookingSaga> stuck = bookingSagaRepository.findByStatusInAndUpdatedAtBefore(
                List.of(SagaStatus.STARTED, SagaStatus.SEATS_HELD), LocalDateTime.now().minusMinutes(staleAfterMinutes));
        for (BookingSaga saga : stuck) {
            bookingEventProducer.sendSeatsReleased(saga.getBookingId(), saga.getBookingReference(), saga.getSeatInstanceIds());
            saga.setStatus(SagaStatus.FAILED);
            saga.setLastError("Abandoned before a booking was created; swept after " + staleAfterMinutes + "m");
        }
        if (!stuck.isEmpty()) {
            bookingSagaRepository.saveAll(stuck);
            log.info("Swept {} abandoned saga(s) that never reached a created booking", stuck.size());
        }
    }

    private void sweepNeverConfirmed() {
        List<BookingSaga> stuck = bookingSagaRepository.findByStatusInAndUpdatedAtBefore(
                List.of(SagaStatus.BOOKING_CREATED, SagaStatus.PAYMENT_INITIATED), LocalDateTime.now().minusMinutes(paymentTimeoutMinutes));
        for (BookingSaga saga : stuck) {
            Booking booking = saga.getBookingId() != null ? bookingRepository.findById(saga.getBookingId()).orElse(null) : null;
            if (booking == null) {
                bookingEventProducer.sendSeatsReleased(saga.getBookingId(), saga.getBookingReference(), saga.getSeatInstanceIds());
                saga.setStatus(SagaStatus.CANCELLED);
            } else if (booking.getStatus() == BookingStatus.PENDING) {
                booking.setStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                bookingEventProducer.sendBookingCancelled(booking, ErrorMessageUtil.BOOKING_CANCELLED_PAYMENT_TIMEOUT);
                if (booking.getTripId() != null) {
                    tripRepository.findById(booking.getTripId())
                            .filter(trip -> trip.getStatus() == BookingStatus.PENDING)
                            .ifPresent(trip -> {
                                trip.setStatus(BookingStatus.CANCELLED);
                                tripRepository.save(trip);
                            });
                }
                saga.setStatus(SagaStatus.CANCELLED);
                saga.setLastError("Payment never confirmed or failed; swept after " + paymentTimeoutMinutes + "m");
            } else {
                saga.setStatus(booking.getStatus() == BookingStatus.CANCELLED ? SagaStatus.CANCELLED : SagaStatus.COMPLETED);
            }
        }
        if (!stuck.isEmpty()) {
            bookingSagaRepository.saveAll(stuck);
            log.info("Swept {} saga(s) whose payment never resolved", stuck.size());
        }
    }
}
