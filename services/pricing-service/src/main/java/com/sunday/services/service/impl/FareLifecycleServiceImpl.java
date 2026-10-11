package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.constants.PricingPermissions;
import com.sunday.common_lib.enums.FareStatus;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.response.FareResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.integration.PricingIntegrationService;
import com.sunday.services.mapper.FareMapper;
import com.sunday.services.model.Fare;
import com.sunday.services.repository.BaggagePolicyRepository;
import com.sunday.services.repository.FareRepository;
import com.sunday.services.repository.FareRulesRepository;
import com.sunday.services.service.FareLifecycleService;
import com.sunday.services.config.FareCacheEvictor;
import com.sunday.services.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Transactional
public class FareLifecycleServiceImpl implements FareLifecycleService {

    private final FareRepository fareRepository;
    private final FareRulesRepository fareRulesRepository;
    private final BaggagePolicyRepository baggagePolicyRepository;
    private final PricingIntegrationService pricingIntegrationService;
    private final Clock clock;

    @Override
    @CacheEvict(cacheNames = RedisConfig.FARES, key = "#id")
    public FareResponse publishFare(UUID userId, UUID id) {
        Fare fare = findOrThrow(id);
        pricingIntegrationService.requirePermission(userId, fare.getAirlineId(), PricingPermissions.FARE_MANAGE);

        if (fare.getStatus() != FareStatus.DRAFT && fare.getStatus() != FareStatus.WITHDRAWN) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.FARE_INVALID_STATUS_TRANSITION, fare.getStatus(), FareStatus.PUBLISHED));
        }
        Instant now = Instant.now(clock);
        if (!fare.getSalesTo().isAfter(now)) {
            throw new ConflictException(ErrorMessageUtil.FARE_SALES_WINDOW_ENDED);
        }
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        if (fare.getTravelTo().isBefore(today)) {
            throw new ConflictException(ErrorMessageUtil.FARE_TRAVEL_WINDOW_ENDED);
        }
        if (!fareRulesRepository.existsByFareId(id) || !baggagePolicyRepository.existsByFareId(id)) {
            throw new ConflictException(ErrorMessageUtil.FARE_TERMS_MISSING);
        }

        fare.setStatus(FareStatus.PUBLISHED);
        fare.setUpdatedBy(userId);
        return FareMapper.toResponse(fareRepository.save(fare));
    }

    @Override
    @CacheEvict(cacheNames = RedisConfig.FARES, key = "#id")
    public FareResponse withdrawFare(UUID userId, UUID id) {
        Fare fare = findOrThrow(id);
        pricingIntegrationService.requirePermission(userId, fare.getAirlineId(), PricingPermissions.FARE_MANAGE);

        if (fare.getStatus() != FareStatus.PUBLISHED) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.FARE_INVALID_STATUS_TRANSITION, fare.getStatus(), FareStatus.WITHDRAWN));
        }
        fare.setStatus(FareStatus.WITHDRAWN);
        fare.setUpdatedBy(userId);
        return FareMapper.toResponse(fareRepository.save(fare));
    }

    private Fare findOrThrow(UUID id) {
        return fareRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.FARE_NOT_FOUND_BY_ID, id)));
    }
}
