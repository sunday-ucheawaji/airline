package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.PricingPermissions;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.FareRulesRequest;
import com.sunday.common_lib.payload.response.FareRulesResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.integration.PricingIntegrationService;
import com.sunday.services.mapper.FareRulesMapper;
import com.sunday.services.model.Fare;
import com.sunday.services.model.FareRules;
import com.sunday.services.repository.FareRepository;
import com.sunday.services.repository.FareRulesRepository;
import com.sunday.services.service.FareRulesService;
import com.sunday.services.config.FareCacheEvictor;
import com.sunday.services.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional
public class FareRulesServiceImpl implements FareRulesService {

    private final FareRulesRepository fareRulesRepository;
    private final FareRepository fareRepository;
    private final PricingIntegrationService pricingIntegrationService;
    private final FareCacheEvictor fareCacheEvictor;

    @Override
    @CacheEvict(cacheNames = RedisConfig.FARES, key = "#request.fareId")
    public FareRulesResponse createFareRules(Long userId, FareRulesRequest request) {
        Fare fare = fareRepository.findById(request.getFareId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FARE_NOT_FOUND_BY_ID, request.getFareId())));
        pricingIntegrationService.requirePermission(userId, fare.getAirlineId(), PricingPermissions.FARE_RULES_MANAGE);

        if (fareRulesRepository.existsByFareId(request.getFareId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.FARE_RULES_ALREADY_EXIST, request.getFareId()));
        }
        return FareRulesMapper.toResponse(fareRulesRepository.save(FareRulesMapper.toEntity(request, fare, userId)));
    }

    @Override
    @Transactional(readOnly = true)
    public FareRulesResponse getFareRulesById(Long userId, Long id) {
        FareRules fareRules = findOrThrow(id);
        pricingIntegrationService.requirePermission(userId, fareRules.getAirlineId(), PricingPermissions.FARE_READ);
        return FareRulesMapper.toResponse(fareRules);
    }

    @Override
    @Transactional(readOnly = true)
    public FareRulesResponse getFareRulesByFareId(Long userId, Long fareId) {
        FareRules fareRules = fareRulesRepository.findByFareId(fareId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FARE_RULES_NOT_FOUND_FOR_FARE, fareId)));
        pricingIntegrationService.requirePermission(userId, fareRules.getAirlineId(), PricingPermissions.FARE_READ);
        return FareRulesMapper.toResponse(fareRules);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FareRulesResponse> getFareRulesByAirlineId(Long userId, Long airlineId, Pageable pageable) {
        pricingIntegrationService.requirePermission(userId, airlineId, PricingPermissions.FARE_READ);
        return fareRulesRepository.findByAirlineId(airlineId, pageable).map(FareRulesMapper::toResponse);
    }

    @Override
    public FareRulesResponse updateFareRules(Long userId, Long id, FareRulesRequest request) {
        FareRules existing = findOrThrow(id);
        pricingIntegrationService.requirePermission(userId, existing.getAirlineId(), PricingPermissions.FARE_RULES_MANAGE);
        FareRulesMapper.updateEntity(request, existing, userId);
        FareRulesResponse saved = FareRulesMapper.toResponse(fareRulesRepository.save(existing));
        fareCacheEvictor.evict(existing.getFare().getId());
        return saved;
    }

    @Override
    public void deleteFareRules(Long userId, Long id) {
        FareRules fareRules = findOrThrow(id);
        pricingIntegrationService.requirePermission(userId, fareRules.getAirlineId(), PricingPermissions.FARE_RULES_MANAGE);
        fareRulesRepository.delete(fareRules);
        fareCacheEvictor.evict(fareRules.getFare().getId());
    }

    private FareRules findOrThrow(Long id) {
        return fareRulesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FARE_RULES_NOT_FOUND_BY_ID, id)));
    }
}
