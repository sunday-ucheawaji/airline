package com.sunday.services.service;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.response.BookingResponse;
import com.sunday.common_lib.payload.response.BookingStatisticsResponse;
import com.sunday.common_lib.payload.response.PassengerResponse;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.common_lib.payload.response.PayableBookingResponse;

import java.util.List;

public interface BookingService {

    PaymentInitiateResponse createBooking(BookingRequest request, Long userId, String idempotencyKey);

    BookingResponse updateBooking(Long id, BookingRequest request, Long userId);

    BookingResponse getBookingById(Long id, Long userId);

    /** Customer-care read by booking reference. Passport and date of birth are masked. Platform permission, enforced at the gateway. */
    BookingResponse getBookingForSupport(String bookingReference);

    List<BookingResponse> getBookingsByAirline(
            Long userId,
            Long airlineId,
            String searchQuery,
            BookingStatus status,
            Long flightInstanceId,
            String sortDirection
    );

    List<BookingResponse> getBookingsByUser(Long userId);

    List<PassengerResponse> getPassengersByBooking(Long bookingId, Long userId);

    BookingResponse cancelBooking(Long id, Long userId);

    void deleteBooking(Long id, Long userId);

    boolean existsById(Long id);

    long count();

    long countByFlightId(Long userId, Long airlineId, Long flightInstanceId);

    BookingStatisticsResponse getBookingStatisticsForAirline(Long userId, Long airlineId);

    /** Service-to-service: what payment-service needs to check a payment against this booking. */
    PayableBookingResponse getPayableBooking(Long id);
}
