package com.sunday.services.service;

import com.sunday.common_lib.event.BookingCancelledEvent;

public interface SeatBookingService {

    /** Frees a cancelled booking's seats, only those it still holds or owns. Deduplicated per booking. */
    void releaseSeatInstances(BookingCancelledEvent event);
}
