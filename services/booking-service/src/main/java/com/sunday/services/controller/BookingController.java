package com.sunday.services.controller;

import java.util.UUID;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.common_lib.payload.request.BookingChangeRequest;
import com.sunday.common_lib.payload.response.ExtrasOrderResponse;
import com.sunday.common_lib.payload.response.TripResponse;
import com.sunday.common_lib.payload.request.ExtrasOrderRequest;
import com.sunday.common_lib.payload.request.TripRequest;
import com.sunday.common_lib.payload.response.BookingChangeQuoteResponse;
import com.sunday.common_lib.payload.request.BookingRequest;
import com.sunday.common_lib.payload.response.BookingResponse;
import com.sunday.common_lib.payload.response.BookingStatisticsResponse;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.services.service.BookingService;
import com.sunday.services.service.ExtrasOrderService;
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
    private final ExtrasOrderService extrasOrderService;

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

    @PostMapping("/trips")
    public ResponseEntity<PaymentInitiateResponse> createTrip(
            @Valid @RequestBody TripRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createTrip(request, userId, idempotencyKey));
    }

    @GetMapping("/trips/{id}")
    public ResponseEntity<TripResponse> getTrip(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.getTrip(id, userId));
    }

    @PatchMapping("/trips/{id}/cancel")
    public ResponseEntity<TripResponse> cancelTrip(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.cancelTrip(id, userId));
    }

    @PostMapping("/{id}/extras")
    public ResponseEntity<ExtrasOrderResponse> orderExtras(
            @PathVariable UUID id,
            @RequestBody ExtrasOrderRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED).body(extrasOrderService.orderExtras(id, request, userId, idempotencyKey));
    }

    @PostMapping("/{id}/change")
    public ResponseEntity<PaymentInitiateResponse> changeBooking(
            @PathVariable UUID id,
            @Valid @RequestBody BookingChangeRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PaymentInitiateResponse response = bookingService.changeBooking(id, request, userId, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/change/quote")
    public ResponseEntity<BookingChangeQuoteResponse> quoteChange(
            @PathVariable UUID id,
            @Valid @RequestBody BookingChangeRequest request,
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(bookingService.quoteChange(id, request, userId));
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
