package com.sunday.services.service;

import com.sunday.common_lib.payload.request.BaggagePolicyRequest;
import com.sunday.common_lib.payload.response.BaggagePolicyBulkCreateResponse;
import com.sunday.common_lib.payload.response.BaggagePolicyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BaggagePolicyService {

    BaggagePolicyResponse createBaggagePolicy(Long userId, BaggagePolicyRequest request);
    BaggagePolicyBulkCreateResponse createBaggagePolicies(Long userId, List<BaggagePolicyRequest> requests);
    BaggagePolicyResponse getBaggagePolicyById(Long userId, Long id);
    BaggagePolicyResponse getBaggagePolicyByFareId(Long userId, Long fareId);
    Page<BaggagePolicyResponse> getBaggagePoliciesByAirlineId(Long userId, Long airlineId, Pageable pageable);
    BaggagePolicyResponse updateBaggagePolicy(Long userId, Long id, BaggagePolicyRequest request);
    void deleteBaggagePolicy(Long userId, Long id);
}
