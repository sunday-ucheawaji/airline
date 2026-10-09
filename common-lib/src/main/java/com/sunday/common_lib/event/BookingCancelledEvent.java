package com.sunday.common_lib.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Published whenever a booking is abandoned before or after confirmation, so its held/booked seats are released. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCancelledEvent {
    private Long bookingId;
    private String bookingReference;
    private List<Long> seatInstanceIds;

    // Contact (booking.contactInfo or first passenger fallback), null when the booking never got far
    // enough to have passengers (e.g. a saga compensation before any Passenger row existed) — consumers
    // (notification-service) must treat a blank contact as "nothing to notify", not an error.
    private String contactEmail;
    private String contactPhone;
    private String reason;
}
