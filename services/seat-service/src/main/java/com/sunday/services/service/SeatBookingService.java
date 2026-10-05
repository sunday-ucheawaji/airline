package com.sunday.services.service;

import com.sunday.common_lib.event.BookingConfirmedEvent;

public interface SeatBookingService {

    void markSeatInstancesBooked(BookingConfirmedEvent event);
}
