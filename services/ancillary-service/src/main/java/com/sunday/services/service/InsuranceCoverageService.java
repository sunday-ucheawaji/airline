package com.sunday.services.service;

import com.sunday.common_lib.payload.request.InsuranceCoverageRequest;
import com.sunday.common_lib.payload.response.InsuranceCoverageBulkCreateResponse;
import com.sunday.common_lib.payload.response.InsuranceCoverageResponse;

import java.util.List;

public interface InsuranceCoverageService {

    InsuranceCoverageResponse createCoverage(Long userId, InsuranceCoverageRequest request);

    InsuranceCoverageBulkCreateResponse createCoveragesBulk(Long userId, List<InsuranceCoverageRequest> requests);

    InsuranceCoverageResponse updateCoverage(Long userId, Long id, InsuranceCoverageRequest request);

    void deleteCoverage(Long userId, Long id);

    InsuranceCoverageResponse getCoverageById(Long id);

    List<InsuranceCoverageResponse> getCoveragesByAncillaryId(Long ancillaryId);

    List<InsuranceCoverageResponse> getActiveCoveragesByAncillaryId(Long ancillaryId);

    List<InsuranceCoverageResponse> getAllCoverages();
}
