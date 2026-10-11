package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.payload.request.FareRulesRequest;
import com.sunday.common_lib.payload.response.FareRulesResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FareRulesService {

    FareRulesResponse createFareRules(UUID userId, FareRulesRequest request);
    FareRulesResponse getFareRulesById(UUID userId, UUID id);
    FareRulesResponse getFareRulesByFareId(UUID userId, UUID fareId);
    Page<FareRulesResponse> getFareRulesByAirlineId(UUID userId, UUID airlineId, Pageable pageable);
    FareRulesResponse updateFareRules(UUID userId, UUID id, FareRulesRequest request);
    void deleteFareRules(UUID userId, UUID id);
}
