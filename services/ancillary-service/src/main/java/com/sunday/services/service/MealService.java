package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.MealRequest;
import com.sunday.common_lib.payload.response.MealBulkCreateResponse;
import com.sunday.common_lib.payload.response.MealResponse;

import java.util.List;

public interface MealService {

    MealResponse create(UUID userId, MealRequest request);

    MealBulkCreateResponse bulkCreate(UUID userId, List<MealRequest> requests);

    MealResponse getById(UUID id);

    /** Batch variant of {@link #getById}: one round trip instead of one per meal. */
    List<MealResponse> getAllByIds(List<UUID> ids);

    List<MealResponse> getByAirlineId(UUID userId, UUID airlineId);

    MealResponse update(UUID userId, UUID id, MealRequest request);

    void delete(UUID userId, UUID id);

    MealResponse updateAvailability(UUID userId, UUID id, Boolean available);
}
