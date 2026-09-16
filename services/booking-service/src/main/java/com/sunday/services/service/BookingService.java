package com.sunday.services.service;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.exception.PaymentException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.response.BookingResponse;
import com.sunday.common_lib.payload.response.BookingStatisticsResponse;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;

import java.util.List;

public interface BookingService {

    PaymentInitiateResponse createBooking(BookingRequest request, Long userId)
            throws ResourceNotFoundException, PaymentException;

    BookingResponse updateBooking(Long id, BookingRequest request)
            throws ResourceNotFoundException;

    BookingResponse getBookingById(Long id) throws ResourceNotFoundException;



    List<BookingResponse> getBookingsByAirline(
            Long userId,
            String searchQuery,
            BookingStatus status,
            Long flightInstanceId,
            String sortDirection
    );

    List<BookingResponse> getBookingsByUser(Long userId);

    BookingResponse cancelBooking(Long id) throws ResourceNotFoundException;

    void deleteBooking(Long id) throws ResourceNotFoundException;

    boolean existsById(Long id);

    long count();

    long countByFlightId(Long flightId);

    BookingStatisticsResponse getBookingStatisticsForAirline(Long airlineId);
}
