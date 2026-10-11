package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.event.BookingCancelledEvent;
import com.sunday.common_lib.event.BookingConfirmedEvent;
import com.sunday.services.model.ProcessedBookingEvent;
import com.sunday.services.repository.ProcessedBookingEventRepository;
import com.sunday.services.service.SeatBookingService;
import com.sunday.services.service.SeatInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeatBookingServiceImpl implements SeatBookingService {

    private final SeatInstanceService seatInstanceService;
    private final ProcessedBookingEventRepository processedBookingEventRepository;

    @Override
    public void markSeatInstancesBooked(BookingConfirmedEvent event) {
        if (event.getSeatInstanceIds() == null || event.getSeatInstanceIds().isEmpty()) {
            log.warn("No seat instance IDs in BookingConfirmedEvent for booking: {}", event.getBookingReference());
            return;
        }
        if (!claimEvent(event.getBookingId(), "CONFIRMED")) {
            return;
        }
        seatInstanceService.markSeatsBookedBatch(event.getBookingReference(), event.getSeatInstanceIds());
        log.info("Seat instances {} marked as BOOKED for booking {}", event.getSeatInstanceIds(), event.getBookingReference());
    }

    @Override
    public void releaseSeatInstances(BookingCancelledEvent event) {
        if (event.getSeatInstanceIds() == null || event.getSeatInstanceIds().isEmpty()) {
            return;
        }
        if (!claimEvent(event.getBookingId(), "CANCELLED")) {
            return;
        }
        seatInstanceService.releaseSeatsBatch(event.getSeatInstanceIds());
        log.info("Seat instances {} released for cancelled booking {}", event.getSeatInstanceIds(), event.getBookingReference());
    }

    /**
     * Deliberately not REQUIRES_NEW: the claim must commit atomically with the seat mutation that
     * follows it, in the same transaction, so a redelivery after a genuine failure can still retry —
     * see BookingSagaServiceImpl.claimPaymentEvent in booking-service for the full rationale behind
     * this exact same shape.
     */
    private boolean claimEvent(UUID bookingId, String eventType) {
        try {
            processedBookingEventRepository.saveAndFlush(
                    ProcessedBookingEvent.builder().bookingId(bookingId).eventType(eventType).build());
            return true;
        } catch (DataIntegrityViolationException e) {
            log.info("Booking event {} for booking {} already processed, skipping redelivery", eventType, bookingId);
            return false;
        }
    }
}
