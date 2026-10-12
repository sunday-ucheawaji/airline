package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.AncillaryRequest;
import com.sunday.common_lib.payload.response.AncillaryBulkCreateResponse;
import com.sunday.common_lib.payload.response.AncillaryResponse;

import java.util.List;

public interface AncillaryService {

    AncillaryResponse create(UUID userId, AncillaryRequest request);

    AncillaryBulkCreateResponse bulkCreate(UUID userId, List<AncillaryRequest> requests);

    AncillaryResponse getById(UUID id);

    List<AncillaryResponse> getAllByAirlineId(UUID userId, UUID airlineId);

    /** Batch variant: one round trip instead of one per ancillary. */
    List<AncillaryResponse> getAllByIds(List<UUID> ids);

    AncillaryResponse update(UUID userId, UUID id, AncillaryRequest request);

    void delete(UUID userId, UUID id);
}
