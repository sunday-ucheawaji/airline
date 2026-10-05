package com.sunday.services.event;

import com.sunday.common_lib.event.BookingConfirmedEvent;
import com.sunday.services.service.SeatBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingEventListener {

    private final SeatBookingService seatBookingService;

    @KafkaListener(topics = "booking.confirmed", groupId = "seat-service-group")
    public void handleBookingConfirmed(BookingConfirmedEvent event) {
        seatBookingService.markSeatInstancesBooked(event);
    }
}
