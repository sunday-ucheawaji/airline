package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.InsuranceCoverageRequest;
import com.sunday.common_lib.payload.response.InsuranceCoverageBulkCreateResponse;
import com.sunday.common_lib.payload.response.InsuranceCoverageResponse;

import java.util.List;

public interface InsuranceCoverageService {

    InsuranceCoverageResponse createCoverage(UUID userId, InsuranceCoverageRequest request);

    InsuranceCoverageBulkCreateResponse createCoveragesBulk(UUID userId, List<InsuranceCoverageRequest> requests);

    InsuranceCoverageResponse updateCoverage(UUID userId, UUID id, InsuranceCoverageRequest request);

    void deleteCoverage(UUID userId, UUID id);

    InsuranceCoverageResponse getCoverageById(UUID id);

    /** Batch variant of {@link #getCoverageById}: one round trip instead of one per coverage. */
    List<InsuranceCoverageResponse> getAllByIds(List<UUID> ids);

    List<InsuranceCoverageResponse> getCoveragesByAncillaryId(UUID ancillaryId);

    List<InsuranceCoverageResponse> getActiveCoveragesByAncillaryId(UUID ancillaryId);

    List<InsuranceCoverageResponse> getAllCoverages();
}
