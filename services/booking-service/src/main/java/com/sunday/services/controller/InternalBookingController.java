package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.payload.response.PayableBookingResponse;
import com.sunday.services.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only, not routed by the gateway. payment-service checks each payment against this. */
@RestController
@RequestMapping("/internal/bookings")
@RequiredArgsConstructor
public class InternalBookingController {

    private final BookingService bookingService;

    @GetMapping("/{id}/payable")
    public PayableBookingResponse getPayableBooking(@PathVariable UUID id) {
        return bookingService.getPayableBooking(id);
    }
}
