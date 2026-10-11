package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.FlightMealRequest;
import com.sunday.common_lib.payload.response.FlightMealBulkCreateResponse;
import com.sunday.common_lib.payload.response.FlightMealResponse;

import java.math.BigDecimal;
import java.util.List;

public interface FlightMealService {

    FlightMealResponse create(UUID userId, FlightMealRequest request);

    FlightMealBulkCreateResponse bulkCreate(UUID userId, List<FlightMealRequest> requests);

    FlightMealResponse getById(UUID id);

    List<FlightMealResponse> getByFlightId(UUID flightId);

    List<FlightMealResponse> getAllByIds(List<UUID> Ids);

    FlightMealResponse update(UUID userId, UUID id, FlightMealRequest request);

    void delete(UUID userId, UUID id);

    FlightMealResponse updateAvailability(UUID userId, UUID id, Boolean available);

    BigDecimal calculateMealPrice(UUID flightId, List<UUID> mealIds);
}
