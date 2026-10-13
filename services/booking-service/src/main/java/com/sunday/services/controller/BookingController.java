package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.response.BookingResponse;
import com.sunday.common_lib.payload.response.BookingStatisticsResponse;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.services.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<PaymentInitiateResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PaymentInitiateResponse response = bookingService.createBooking(request, userId, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable UUID id,
            @Valid @RequestBody BookingRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        BookingResponse response = bookingService.updateBooking(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        BookingResponse response = bookingService.getBookingById(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/support/{bookingReference}")
    public ResponseEntity<BookingResponse> getBookingForSupport(
            @PathVariable String bookingReference) {
        return ResponseEntity.ok(bookingService.getBookingForSupport(bookingReference));
    }

    @GetMapping("/airline")
    public ResponseEntity<Page<BookingResponse>> getBookingsByAirline(
            @RequestParam UUID airlineId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) UUID flightInstanceId,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.getBookingsByAirline(
                userId, airlineId, search, status, flightInstanceId, sortDirection, page, size));
    }

    @GetMapping("/user/history")
    public ResponseEntity<Page<BookingResponse>> getBookingsByUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.getBookingsByUser(userId, page, size));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId) {
        BookingResponse response = bookingService.cancelBooking(id, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count/flight/{flightInstanceId}")
    public ResponseEntity<Long> getBookingCountByFlight(
            @PathVariable UUID flightInstanceId,
            @RequestParam UUID airlineId,
            @RequestHeader("X-User-Id") UUID userId) {
        long count = bookingService.countByFlightId(userId, airlineId, flightInstanceId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/statistics/airline")
    public ResponseEntity<BookingStatisticsResponse> getBookingStatisticsForAirline(
            @RequestParam UUID airlineId,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.getBookingStatisticsForAirline(userId, airlineId));
    }
}
