package com.sunday.services.service;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.response.BookingResponse;
import com.sunday.common_lib.payload.response.BookingStatisticsResponse;
import com.sunday.common_lib.payload.response.PassengerResponse;
import com.sunday.common_lib.payload.response.PayableBookingResponse;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    PaymentInitiateResponse createBooking(BookingRequest request, UUID userId, String idempotencyKey);

    BookingResponse updateBooking(UUID id, BookingRequest request, UUID userId);

    BookingResponse getBookingById(UUID id, UUID userId);

    BookingResponse getBookingForSupport(String bookingReference);

    Page<BookingResponse> getBookingsByAirline(
            UUID userId,
            UUID airlineId,
            String searchQuery,
            BookingStatus status,
            UUID flightInstanceId,
            String sortDirection,
            int page,
            int size
    );

    Page<BookingResponse> getBookingsByUser(UUID userId, int page, int size);

    List<PassengerResponse> getPassengersByBooking(UUID bookingId, UUID userId);

    BookingResponse cancelBooking(UUID id, UUID userId);

    long countByFlightId(UUID userId, UUID airlineId, UUID flightInstanceId);

    BookingStatisticsResponse getBookingStatisticsForAirline(UUID userId, UUID airlineId);

    PayableBookingResponse getPayableBooking(UUID id);
}
