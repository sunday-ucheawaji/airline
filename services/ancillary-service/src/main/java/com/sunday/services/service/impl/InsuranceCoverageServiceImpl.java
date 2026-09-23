package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.AncillaryPermissions;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.InsuranceCoverageRequest;
import com.sunday.common_lib.payload.response.InsuranceCoverageBulkCreateResponse;
import com.sunday.common_lib.payload.response.InsuranceCoverageResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.mapper.InsuranceCoverageMapper;
import com.sunday.services.model.Ancillary;
import com.sunday.services.model.InsuranceCoverage;
import com.sunday.services.repository.AncillaryRepository;
import com.sunday.services.repository.InsuranceCoverageRepository;
import com.sunday.services.service.InsuranceCoverageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsuranceCoverageServiceImpl implements InsuranceCoverageService {

    private final InsuranceCoverageRepository coverageRepository;
    private final AncillaryRepository ancillaryRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    @Transactional
    public InsuranceCoverageResponse createCoverage(Long userId, InsuranceCoverageRequest request) {
        Ancillary ancillary = ancillaryRepository.findById(request.getAncillaryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.ANCILLARY_NOT_FOUND_BY_ID, request.getAncillaryId())));
        airlineIntegrationService.requirePermission(
                userId, ancillary.getAirlineId(), AncillaryPermissions.INSURANCE_MANAGE);

        InsuranceCoverage coverage = InsuranceCoverageMapper.toEntity(request, ancillary);
        InsuranceCoverage saved = coverageRepository.save(coverage);
        return InsuranceCoverageMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public InsuranceCoverageBulkCreateResponse createCoveragesBulk(Long userId, List<InsuranceCoverageRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return InsuranceCoverageBulkCreateResponse.builder().created(List.of()).skipped(List.of()).build();
        }

        List<Long> ancillaryIds = requests.stream()
                .map(InsuranceCoverageRequest::getAncillaryId)
                .distinct()
                .toList();
        Map<Long, Ancillary> ancillaryById = ancillaryRepository.findAllById(ancillaryIds).stream()
                .collect(Collectors.toMap(Ancillary::getId, a -> a));

        Set<Long> airlineIds = ancillaryById.values().stream()
                .map(Ancillary::getAirlineId)
                .collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, AncillaryPermissions.INSURANCE_MANAGE);

        List<InsuranceCoverageBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();
        List<InsuranceCoverage> toInsert = new ArrayList<>();
        for (InsuranceCoverageRequest request : requests) {
            Ancillary ancillary = ancillaryById.get(request.getAncillaryId());
            if (ancillary == null) {
                skipped.add(InsuranceCoverageBulkCreateResponse.SkippedRequest.builder()
                        .request(request)
                        .reason(String.format(ErrorMessageUtil.ANCILLARY_NOT_FOUND_BY_ID, request.getAncillaryId()))
                        .build());
                continue;
            }
            toInsert.add(InsuranceCoverageMapper.toEntity(request, ancillary));
        }

        List<InsuranceCoverageResponse> created = coverageRepository.saveAll(toInsert).stream()
                .map(InsuranceCoverageMapper::toResponse)
                .collect(Collectors.toList());

        return InsuranceCoverageBulkCreateResponse.builder().created(created).skipped(skipped).build();
    }

    @Override
    @Transactional
    public InsuranceCoverageResponse updateCoverage(Long userId, Long id, InsuranceCoverageRequest request) {
        InsuranceCoverage existing = coverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.INSURANCE_COVERAGE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, existing.getAncillary().getAirlineId(), AncillaryPermissions.INSURANCE_MANAGE);

        Ancillary ancillary = null;
        if (request.getAncillaryId() != null) {
            ancillary = ancillaryRepository.findById(request.getAncillaryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ErrorMessageUtil.ANCILLARY_NOT_FOUND_BY_ID, request.getAncillaryId())));
            // Re-parenting to an ancillary under a different airline needs permission there too
            if (!ancillary.getAirlineId().equals(existing.getAncillary().getAirlineId())) {
                airlineIntegrationService.requirePermission(
                        userId, ancillary.getAirlineId(), AncillaryPermissions.INSURANCE_MANAGE);
            }
        }

        InsuranceCoverageMapper.updateEntityFromRequest(existing, request, ancillary);
        InsuranceCoverage updated = coverageRepository.save(existing);
        return InsuranceCoverageMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCoverage(Long userId, Long id) {
        InsuranceCoverage coverage = coverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.INSURANCE_COVERAGE_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, coverage.getAncillary().getAirlineId(), AncillaryPermissions.INSURANCE_MANAGE);
        coverageRepository.delete(coverage);
    }

    @Override
    public InsuranceCoverageResponse getCoverageById(Long id) {
        InsuranceCoverage coverage = coverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.INSURANCE_COVERAGE_NOT_FOUND_BY_ID, id)));
        return InsuranceCoverageMapper.toResponse(coverage);
    }

    @Override
    public List<InsuranceCoverageResponse> getCoveragesByAncillaryId(Long ancillaryId) {
        return coverageRepository.findByAncillaryIdAndActiveTrue(ancillaryId).stream()
                .map(InsuranceCoverageMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InsuranceCoverageResponse> getActiveCoveragesByAncillaryId(Long ancillaryId) {
        return coverageRepository.findByAncillaryIdAndActiveTrue(ancillaryId).stream()
                .map(InsuranceCoverageMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InsuranceCoverageResponse> getAllCoverages() {
        return coverageRepository.findAll().stream()
                .map(InsuranceCoverageMapper::toResponse)
                .collect(Collectors.toList());
    }
}
