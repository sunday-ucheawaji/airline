package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.request.SeatInstanceRequest;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;

import java.math.BigDecimal;
import java.util.List;

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

    /** Atomically holds every requested seat (exists, belongs to the flight instance, right cabin class, AVAILABLE) or none of them. */
    SeatHoldResponse holdSeats(SeatHoldRequest request);

    /** Confirms a booking's seats: HELD or still-AVAILABLE -> BOOKED. Logs loudly if a seat was taken by someone else. */
    void markSeatsBookedBatch(String bookingReference, List<UUID> seatInstanceIds);

    /** Releases an abandoned booking's seats (HELD or BOOKED) back to AVAILABLE. Idempotent. */
    void releaseSeatsBatch(List<UUID> seatInstanceIds);
}
