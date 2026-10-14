package com.sunday.services.service.impl;

import java.util.UUID;
import com.sunday.common_lib.constants.PricingPermissions;
import com.sunday.common_lib.enums.FareStatus;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.BaggagePolicyRequest;
import com.sunday.common_lib.payload.response.BaggagePolicyBulkCreateResponse;
import com.sunday.common_lib.payload.response.BaggagePolicyBulkCreateResponse.SkippedRequest;
import com.sunday.common_lib.payload.response.BaggagePolicyResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.integration.PricingIntegrationService;
import com.sunday.services.mapper.BaggagePolicyMapper;
import com.sunday.services.model.BaggagePolicy;
import com.sunday.services.model.Fare;
import com.sunday.services.repository.BaggagePolicyRepository;
import com.sunday.services.repository.FareRepository;
import com.sunday.services.service.BaggagePolicyService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import com.sunday.common_lib.util.CacheUtil;
import com.sunday.services.config.FareCacheEvictor;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BaggagePolicyServiceImpl implements BaggagePolicyService {

    public static final int MAX_BULK_ROWS = 500;

    private final BaggagePolicyRepository baggagePolicyRepository;
    private final FareRepository fareRepository;
    private final PricingIntegrationService pricingIntegrationService;
    private final FareCacheEvictor fareCacheEvictor;
    private final Validator validator;

    @Override
    @CacheEvict(cacheNames = CacheUtil.FARES, key = "#request.fareId")
    public BaggagePolicyResponse createBaggagePolicy(UUID userId, BaggagePolicyRequest request) {
        Fare fare = fareRepository.findById(request.getFareId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FARE_NOT_FOUND_BY_ID, request.getFareId())));
        pricingIntegrationService.requirePermission(userId, fare.getAirlineId(), PricingPermissions.FARE_MANAGE);

        if (baggagePolicyRepository.existsByFareId(request.getFareId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.BAGGAGE_POLICY_ALREADY_EXISTS, request.getFareId()));
        }
        return BaggagePolicyMapper.toResponse(
                baggagePolicyRepository.save(BaggagePolicyMapper.toEntity(request, fare, userId)));
    }

    /**
     * Partial success, as for fares: each row is checked in memory (bean validation first). Rows whose fare is
     * unknown, that already have a policy, or that repeat an earlier row's fare are reported in {@code skipped}.
     * The fare lookup, the permission check and the existing-policy lookup each run once for the whole request.
     */
    @Override
    public BaggagePolicyBulkCreateResponse createBaggagePolicies(UUID userId, List<BaggagePolicyRequest> requests) {
        if (requests.size() > MAX_BULK_ROWS) {
            throw new BadRequestException(String.format(ErrorMessageUtil.BULK_TOO_LARGE, MAX_BULK_ROWS));
        }

        List<SkippedRequest> skipped = new ArrayList<>();
        List<IndexedRequest> candidates = new ArrayList<>();
        for (int index = 0; index < requests.size(); index++) {
            BaggagePolicyRequest request = requests.get(index);
            Optional<String> problem = problemWith(request);
            if (problem.isPresent()) {
                skipped.add(skip(index, request, problem.get()));
            } else {
                candidates.add(new IndexedRequest(index, request));
            }
        }

        Set<UUID> requestedFareIds = candidates.stream()
                .map(candidate -> candidate.request().getFareId())
                .collect(Collectors.toSet());
        Map<UUID, Fare> fareById = fareRepository.findAllById(requestedFareIds).stream()
                .collect(Collectors.toMap(Fare::getId, fare -> fare));

        List<IndexedRequest> known = new ArrayList<>();
        for (IndexedRequest candidate : candidates) {
            UUID fareId = candidate.request().getFareId();
            if (fareById.containsKey(fareId)) {
                known.add(candidate);
            } else {
                skipped.add(skip(candidate.index(), candidate.request(),
                        String.format(ErrorMessageUtil.FARE_NOT_FOUND_BY_ID, fareId)));
            }
        }

        List<BaggagePolicyResponse> created = List.of();
        if (!known.isEmpty()) {
            Set<UUID> knownAirlineIds = known.stream()
                    .map(candidate -> fareById.get(candidate.request().getFareId()).getAirlineId())
                    .collect(Collectors.toSet());
            pricingIntegrationService.requirePermission(userId, knownAirlineIds, PricingPermissions.FARE_MANAGE);

            Set<UUID> knownFareIds = known.stream().map(c -> c.request().getFareId()).collect(Collectors.toSet());
            Set<UUID> alreadyHasPolicy = baggagePolicyRepository.findFareIdsWithExistingPolicy(knownFareIds);

            Set<UUID> seenFareIds = new HashSet<>();
            List<BaggagePolicy> toSave = new ArrayList<>();
            for (IndexedRequest candidate : known) {
                BaggagePolicyRequest request = candidate.request();
                UUID fareId = request.getFareId();
                if (alreadyHasPolicy.contains(fareId)) {
                    skipped.add(skip(candidate.index(), request, String.format(ErrorMessageUtil.BAGGAGE_POLICY_ALREADY_EXISTS, fareId)));
                } else if (!seenFareIds.add(fareId)) {
                    skipped.add(skip(candidate.index(), request, ErrorMessageUtil.BAGGAGE_POLICY_DUPLICATE_IN_REQUEST));
                } else {
                    toSave.add(BaggagePolicyMapper.toEntity(request, fareById.get(fareId), userId));
                }
            }
            created = baggagePolicyRepository.saveAll(toSave).stream()
                    .map(BaggagePolicyMapper::toResponse)
                    .collect(Collectors.toList());
            created.forEach(policy -> fareCacheEvictor.evict(policy.getFareId()));
        }

        skipped.sort(Comparator.comparingInt(SkippedRequest::getIndex));
        return BaggagePolicyBulkCreateResponse.builder().created(created).skipped(skipped).build();
    }

    @Override
    @Transactional(readOnly = true)
    public BaggagePolicyResponse getBaggagePolicyById(UUID userId, UUID id) {
        BaggagePolicy policy = findOrThrow(id);
        pricingIntegrationService.requirePermission(userId, policy.getAirlineId(), PricingPermissions.FARE_READ);
        return BaggagePolicyMapper.toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public BaggagePolicyResponse getBaggagePolicyByFareId(UUID userId, UUID fareId) {
        BaggagePolicy policy = baggagePolicyRepository.findByFareId(fareId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.BAGGAGE_POLICY_NOT_FOUND_FOR_FARE, fareId)));
        pricingIntegrationService.requirePermission(userId, policy.getAirlineId(), PricingPermissions.FARE_READ);
        return BaggagePolicyMapper.toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BaggagePolicyResponse> getBaggagePoliciesByAirlineId(UUID userId, UUID airlineId, Pageable pageable) {
        pricingIntegrationService.requirePermission(userId, airlineId, PricingPermissions.FARE_READ);
        return baggagePolicyRepository.findByAirlineId(airlineId, pageable).map(BaggagePolicyMapper::toResponse);
    }

    @Override
    public BaggagePolicyResponse updateBaggagePolicy(UUID userId, UUID id, BaggagePolicyRequest request) {
        BaggagePolicy existing = findOrThrow(id);
        pricingIntegrationService.requirePermission(userId, existing.getAirlineId(), PricingPermissions.FARE_MANAGE);
        requireNotPublished(existing.getFare());
        BaggagePolicyMapper.updateEntity(request, existing, userId);
        BaggagePolicyResponse saved = BaggagePolicyMapper.toResponse(baggagePolicyRepository.save(existing));
        fareCacheEvictor.evict(existing.getFare().getId());
        return saved;
    }

    @Override
    public void deleteBaggagePolicy(UUID userId, UUID id) {
        BaggagePolicy policy = findOrThrow(id);
        pricingIntegrationService.requirePermission(userId, policy.getAirlineId(), PricingPermissions.FARE_MANAGE);
        Fare fare = policy.getFare();
        requireNotPublished(fare);
        fare.setBaggagePolicy(null);
        baggagePolicyRepository.delete(policy);
        fareCacheEvictor.evict(fare.getId());
    }

    private record IndexedRequest(int index, BaggagePolicyRequest request) {}

    private Optional<String> problemWith(BaggagePolicyRequest request) {
        Set<ConstraintViolation<BaggagePolicyRequest>> violations = validator.validate(request);
        return violations.isEmpty() ? Optional.empty() : Optional.of(violations.iterator().next().getMessage());
    }

    private static SkippedRequest skip(int index, BaggagePolicyRequest request, String reason) {
        return SkippedRequest.builder().index(index).request(request).reason(reason).build();
    }

    private static void requireNotPublished(Fare fare) {
        if (fare.getStatus() == FareStatus.PUBLISHED) {
            throw new ConflictException(String.format(ErrorMessageUtil.FARE_LOCKED_WHILE_PUBLISHED, fare.getId()));
        }
    }

    private BaggagePolicy findOrThrow(UUID id) {
        return baggagePolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.BAGGAGE_POLICY_NOT_FOUND_BY_ID, id)));
    }
}
