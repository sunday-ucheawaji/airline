package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.response.BookingResponse;
import com.sunday.common_lib.payload.response.BookingStatisticsResponse;
import com.sunday.common_lib.payload.response.PassengerResponse;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.common_lib.payload.response.PayableBookingResponse;

import java.util.List;

public interface BookingService {

    PaymentInitiateResponse createBooking(BookingRequest request, UUID userId, String idempotencyKey);

    BookingResponse updateBooking(UUID id, BookingRequest request, UUID userId);

    BookingResponse getBookingById(UUID id, UUID userId);

    /** Customer-care read by booking reference. Passport and date of birth are masked. Platform permission, enforced at the gateway. */
    BookingResponse getBookingForSupport(String bookingReference);

    List<BookingResponse> getBookingsByAirline(
            UUID userId,
            UUID airlineId,
            String searchQuery,
            BookingStatus status,
            UUID flightInstanceId,
            String sortDirection
    );

    List<BookingResponse> getBookingsByUser(UUID userId);

    List<PassengerResponse> getPassengersByBooking(UUID bookingId, UUID userId);

    BookingResponse cancelBooking(UUID id, UUID userId);

    void deleteBooking(UUID id, UUID userId);

    boolean existsById(UUID id);

    long count();

    long countByFlightId(UUID userId, UUID airlineId, UUID flightInstanceId);

    BookingStatisticsResponse getBookingStatisticsForAirline(UUID userId, UUID airlineId);

    /** Service-to-service: what payment-service needs to check a payment against this booking. */
    PayableBookingResponse getPayableBooking(UUID id);
}
