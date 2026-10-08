package com.sunday.services.service;

import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.payload.request.SeatHoldRequest;
import com.sunday.common_lib.payload.request.SeatInstanceRequest;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.SeatHoldResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;

import java.math.BigDecimal;
import java.util.List;

public interface SeatInstanceService {

    SeatInstanceResponse createSeatInstance(Long userId, SeatInstanceRequest request);
    SeatInstanceResponse getSeatInstanceById(Long userId, Long id);
    List<SeatInstanceResponse> getSeatInstancesByFlightId(Long userId, Long flightId);
    List<SeatInstanceResponse> getAvailableSeatsByFlightId(Long userId, Long flightId);
    List<SeatInstanceResponse> getAllByIds(Long userId, List<Long> ids);
    List<SeatInstanceResponse> getAllByIdsInternal(List<Long> ids);
    BigDecimal calculateSeatPriceInternal(List<Long> seatInstanceIds);
    Long countAvailableByFlightId(Long userId, Long flightId);
    BigDecimal calculateSeatPrice(Long userId, List<Long> seatInstanceIds);

    SeatInstanceResponse updateSeatInstanceStatus(Long userId, Long id, SeatAvailabilityStatus status);

    SeatInstanceResponse releaseSeatInstance(Long userId, Long id);

    List<CabinSeatStatusResponse> getSeatStatusSummary(Long flightInstanceId);

    /** Atomically holds every requested seat (exists, belongs to the flight instance, right cabin class, AVAILABLE) or none of them. */
    SeatHoldResponse holdSeats(SeatHoldRequest request);

    /** Confirms a booking's seats: HELD or still-AVAILABLE -> BOOKED. Logs loudly if a seat was taken by someone else. */
    void markSeatsBookedBatch(String bookingReference, List<Long> seatInstanceIds);

    /** Releases an abandoned booking's seats (HELD or BOOKED) back to AVAILABLE. Idempotent. */
    void releaseSeatsBatch(List<Long> seatInstanceIds);
}
