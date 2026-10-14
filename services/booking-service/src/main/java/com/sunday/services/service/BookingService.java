package com.sunday.services.service;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.payload.request.BookingChangeRequest;
import com.sunday.common_lib.payload.response.TripResponse;
import com.sunday.common_lib.payload.request.TripRequest;
import com.sunday.common_lib.payload.response.BookingChangeQuoteResponse;
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

    /**
     * Moves a confirmed booking to another departure of the same airline, under the fare's change rules. Creates and
     * charges a replacement booking (new price plus change fees); once it is paid, the original is cancelled and
     * refunded its value, all of it for a refundable fare, otherwise only what the replacement uses.
     */
    PaymentInitiateResponse changeBooking(UUID id, BookingChangeRequest request, UUID userId, String idempotencyKey);

    /**
     * A round trip or multi-city journey: one booking per flight, created together and paid with one payment.
     * All flights or none: if any flight can't be reserved, the others are released.
     */
    PaymentInitiateResponse createTrip(TripRequest request, UUID userId, String idempotencyKey);

    /** The trip and its bookings in travel order. Owner, or BOOKING_READ on its airline. */
    TripResponse getTrip(UUID id, UUID userId);

    /** Cancels every open flight of the trip, each under its own fare rules (refunds against the trip's payment). */
    TripResponse cancelTrip(UUID id, UUID userId);

    /** Prices a change without holding seats or charging anything. Same checks as {@link #changeBooking}. */
    BookingChangeQuoteResponse quoteChange(UUID id, BookingChangeRequest request, UUID userId);

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
