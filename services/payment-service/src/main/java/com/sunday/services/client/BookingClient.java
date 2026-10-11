package com.sunday.services.client;

import java.util.UUID;
import com.sunday.common_lib.payload.response.PayableBookingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** No fallback: a payment must not start unless the booking's owner and total have been confirmed. */
@FeignClient(name = "booking-service")
public interface BookingClient {

    @GetMapping("/internal/bookings/{id}/payable")
    PayableBookingResponse getPayableBooking(@PathVariable UUID id);
}
