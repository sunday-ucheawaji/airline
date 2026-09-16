package com.sunday.services.service;

import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.MealRequest;
import com.sunday.common_lib.payload.response.MealResponse;

import java.util.List;

public interface MealService {

    MealResponse create(Long userId, MealRequest request) throws ResourceNotFoundException;

    List<MealResponse> bulkCreate(Long userId, List<MealRequest> requests) throws ResourceNotFoundException;

    MealResponse getById(Long id) throws ResourceNotFoundException;

    List<MealResponse> getByAirlineId(Long userId);

    MealResponse update(Long userId, Long id, MealRequest request) throws ResourceNotFoundException;

    void delete(Long id) throws ResourceNotFoundException;

    MealResponse updateAvailability(Long id, Boolean available) throws ResourceNotFoundException;


}
