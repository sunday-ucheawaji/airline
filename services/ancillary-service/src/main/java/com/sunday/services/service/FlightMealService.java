package com.sunday.services.service;

import com.sunday.common_lib.payload.request.FlightMealRequest;
import com.sunday.common_lib.payload.response.FlightMealBulkCreateResponse;
import com.sunday.common_lib.payload.response.FlightMealResponse;

import java.util.List;

public interface FlightMealService {

    FlightMealResponse create(Long userId, FlightMealRequest request);

    FlightMealBulkCreateResponse bulkCreate(Long userId, List<FlightMealRequest> requests);

    FlightMealResponse getById(Long id);

    List<FlightMealResponse> getByFlightId(Long flightId);

    List<FlightMealResponse> getAllByIds(List<Long> Ids);

    FlightMealResponse update(Long userId, Long id, FlightMealRequest request);

    void delete(Long userId, Long id);

    FlightMealResponse updateAvailability(Long userId, Long id, Boolean available);

    Double calculateMealPrice(List<Long> mealIds);
}
