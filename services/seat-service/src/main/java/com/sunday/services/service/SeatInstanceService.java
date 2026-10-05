package com.sunday.services.service;

import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.payload.request.SeatInstanceRequest;
import com.sunday.common_lib.payload.response.CabinSeatStatusResponse;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;

import java.util.List;

public interface SeatInstanceService {

    SeatInstanceResponse createSeatInstance(Long userId, SeatInstanceRequest request);
    SeatInstanceResponse getSeatInstanceById(Long userId, Long id);
    List<SeatInstanceResponse> getSeatInstancesByFlightId(Long userId, Long flightId);
    List<SeatInstanceResponse> getAvailableSeatsByFlightId(Long userId, Long flightId);
    List<SeatInstanceResponse> getAllByIds(Long userId, List<Long> ids);
    Long countAvailableByFlightId(Long userId, Long flightId);
    Double calculateSeatPrice(Long userId, List<Long> seatInstanceIds);

    SeatInstanceResponse updateSeatInstanceStatus(Long userId, Long id, SeatAvailabilityStatus status);

    SeatInstanceResponse updateSeatInstanceStatusInternal(Long id, SeatAvailabilityStatus status);

    SeatInstanceResponse releaseSeatInstance(Long userId, Long id);

    List<CabinSeatStatusResponse> getSeatStatusSummary(Long flightInstanceId);
}
