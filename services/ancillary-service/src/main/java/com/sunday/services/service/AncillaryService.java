package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AncillaryRequest;
import com.sunday.common_lib.payload.response.AncillaryBulkCreateResponse;
import com.sunday.common_lib.payload.response.AncillaryResponse;

import java.util.List;

public interface AncillaryService {

    AncillaryResponse create(Long userId, AncillaryRequest request);

    AncillaryBulkCreateResponse bulkCreate(Long userId, List<AncillaryRequest> requests);

    AncillaryResponse getById(Long id);

    List<AncillaryResponse> getAllByAirlineId(Long userId, Long airlineId);

    AncillaryResponse update(Long userId, Long id, AncillaryRequest request);

    void delete(Long userId, Long id);
}
