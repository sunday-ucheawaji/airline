package com.sunday.services.service;

import com.sunday.common_lib.payload.request.FareRulesRequest;
import com.sunday.common_lib.payload.response.FareRulesResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FareRulesService {

    FareRulesResponse createFareRules(Long userId, FareRulesRequest request);
    FareRulesResponse getFareRulesById(Long userId, Long id);
    FareRulesResponse getFareRulesByFareId(Long userId, Long fareId);
    Page<FareRulesResponse> getFareRulesByAirlineId(Long userId, Long airlineId, Pageable pageable);
    FareRulesResponse updateFareRules(Long userId, Long id, FareRulesRequest request);
    void deleteFareRules(Long userId, Long id);
}
