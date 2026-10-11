package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.BaggagePolicyRequest;
import com.sunday.common_lib.payload.response.BaggagePolicyBulkCreateResponse;
import com.sunday.common_lib.payload.response.BaggagePolicyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BaggagePolicyService {

    BaggagePolicyResponse createBaggagePolicy(UUID userId, BaggagePolicyRequest request);
    BaggagePolicyBulkCreateResponse createBaggagePolicies(UUID userId, List<BaggagePolicyRequest> requests);
    BaggagePolicyResponse getBaggagePolicyById(UUID userId, UUID id);
    BaggagePolicyResponse getBaggagePolicyByFareId(UUID userId, UUID fareId);
    Page<BaggagePolicyResponse> getBaggagePoliciesByAirlineId(UUID userId, UUID airlineId, Pageable pageable);
    BaggagePolicyResponse updateBaggagePolicy(UUID userId, UUID id, BaggagePolicyRequest request);
    void deleteBaggagePolicy(UUID userId, UUID id);
}
