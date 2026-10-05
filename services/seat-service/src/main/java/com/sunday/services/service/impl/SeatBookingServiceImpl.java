package com.sunday.services.service.impl;

import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.event.BookingConfirmedEvent;
import com.sunday.services.service.SeatBookingService;
import com.sunday.services.service.SeatInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SeatBookingServiceImpl implements SeatBookingService {

    private final SeatInstanceService seatInstanceService;

    @Override
    public void markSeatInstancesBooked(BookingConfirmedEvent event) {
        if (event.getSeatInstanceIds() == null || event.getSeatInstanceIds().isEmpty()) {
            log.warn("No seat instance IDs in BookingConfirmedEvent for booking: {}", event.getBookingReference());
            return;
        }

        for (Long seatInstanceId : event.getSeatInstanceIds()) {
            seatInstanceService.updateSeatInstanceStatusInternal(seatInstanceId, SeatAvailabilityStatus.BOOKED);
            log.info("Seat instance {} marked as BOOKED for booking {}", seatInstanceId, event.getBookingReference());
        }
    }
}
