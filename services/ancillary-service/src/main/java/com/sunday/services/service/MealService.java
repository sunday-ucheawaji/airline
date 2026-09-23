package com.sunday.services.service;

import com.sunday.common_lib.payload.request.MealRequest;
import com.sunday.common_lib.payload.response.MealBulkCreateResponse;
import com.sunday.common_lib.payload.response.MealResponse;

import java.util.List;

public interface MealService {

    MealResponse create(Long userId, MealRequest request);

    MealBulkCreateResponse bulkCreate(Long userId, List<MealRequest> requests);

    MealResponse getById(Long id);

    List<MealResponse> getByAirlineId(Long userId, Long airlineId);

    MealResponse update(Long userId, Long id, MealRequest request);

    void delete(Long userId, Long id);

    MealResponse updateAvailability(Long userId, Long id, Boolean available);
}
