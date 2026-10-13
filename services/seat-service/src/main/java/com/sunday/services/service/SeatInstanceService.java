package com.sunday.services.service;

import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.payload.request.SeatConfirmRequest;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.request.SeatInstanceRequest;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.CustomerSeatResponse;
import com.sunday.common_lib.payload.response.SeatConfirmResponse;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface SeatInstanceService {

    SeatInstanceResponse createSeatInstance(UUID userId, SeatInstanceRequest request);

    SeatInstanceResponse getSeatInstanceById(UUID userId, UUID id);

    List<SeatInstanceResponse> getSeatInstancesByFlightId(UUID userId, UUID flightId);

    List<SeatInstanceResponse> getAvailableSeatsByFlightId(UUID userId, UUID flightId);

    List<SeatInstanceResponse> getAllByIds(UUID userId, List<UUID> ids);

    List<SeatInstanceResponse> getAllByIdsInternal(List<UUID> ids);

    BigDecimal calculateSeatPriceInternal(List<UUID> seatInstanceIds);

    Long countAvailableByFlightId(UUID userId, UUID flightId);

    BigDecimal calculateSeatPrice(UUID userId, List<UUID> seatInstanceIds);

    SeatInstanceResponse updateSeatInstanceStatus(UUID userId, UUID id, SeatAvailabilityStatus status);

    SeatInstanceResponse releaseSeatInstance(UUID userId, UUID id);

    List<CabinSeatStatusResponse> getSeatStatusSummary(UUID flightInstanceId);

    /** Every seat of a flight instance as a customer may see it, for choosing seats. */
    List<CustomerSeatResponse> getCustomerSeatMap(UUID flightInstanceId);

    /** Atomically holds every requested seat (exists, belongs to the flight instance, right cabin class, AVAILABLE) or none of them. */
    SeatHoldResponse holdSeats(SeatHoldRequest request);

    /**
     * Books a paid booking's seats. A seat lost since it was held is replaced by a free seat in the same cabin; if the
     * cabin has none left, nothing is booked, the booking's held seats are released and {@code allocated} is false.
     */
    SeatConfirmResponse confirmSeats(SeatConfirmRequest request);

    /** Releases a cancelled booking's seats, but only those still held or booked by that booking. Idempotent. */
    void releaseSeatsForBooking(String bookingReference, List<UUID> seatInstanceIds);
}
