package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.AncillaryPermissions;
import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.AncillaryRequest;
import com.sunday.common_lib.payload.response.AncillaryBulkCreateResponse;
import com.sunday.common_lib.payload.response.AncillaryResponse;
import com.sunday.common_lib.payload.response.InsuranceCoverageResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.mapper.AncillaryMapper;
import com.sunday.services.mapper.InsuranceCoverageMapper;
import com.sunday.services.model.Ancillary;
import com.sunday.services.model.InsuranceCoverage;
import com.sunday.services.repository.AncillaryRepository;
import com.sunday.services.repository.InsuranceCoverageRepository;
import com.sunday.services.service.AncillaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AncillaryServiceImpl implements AncillaryService {

    private final AncillaryRepository ancillaryRepository;
    private final InsuranceCoverageRepository insuranceCoverageRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    public AncillaryResponse create(Long userId, AncillaryRequest request) {
        Long airlineId = request.getAirlineId();
        if (airlineId == null) {
            throw new BadRequestException(ErrorMessageUtil.AIRLINE_ID_REQUIRED);
        }
        airlineIntegrationService.requirePermission(userId, airlineId, AncillaryPermissions.MANAGE);

        Ancillary ancillary = Ancillary.builder()
                .type(request.getType())
                .subType(request.getSubType())
                .rfisc(request.getRfisc())
                .name(request.getName())
                .description(request.getDescription())
                .metadata(request.getMetadata())
                .displayOrder(request.getDisplayOrder())
                .airlineId(airlineId)
                .build();

        Ancillary saved = ancillaryRepository.save(ancillary);
        return AncillaryMapper.toResponse(saved, null);
    }

    @Override
    @Transactional
    public AncillaryBulkCreateResponse bulkCreate(Long userId, List<AncillaryRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return AncillaryBulkCreateResponse.builder()
                    .created(List.of())
                    .skipped(List.of())
                    .build();
        }

        List<AncillaryBulkCreateResponse.SkippedRequest> skipped = new ArrayList<>();
        List<AncillaryRequest> candidates = new ArrayList<>();
        for (AncillaryRequest req : requests) {
            if (req.getAirlineId() == null) {
                skipped.add(AncillaryBulkCreateResponse.SkippedRequest.builder()
                        .request(req)
                        .reason(ErrorMessageUtil.AIRLINE_ID_REQUIRED)
                        .build());
            } else {
                candidates.add(req);
            }
        }

        Set<Long> airlineIds = candidates.stream().map(AncillaryRequest::getAirlineId).collect(Collectors.toSet());
        airlineIntegrationService.requirePermission(userId, airlineIds, AncillaryPermissions.MANAGE);

        List<Ancillary> toInsert = candidates.stream()
                .map(req -> Ancillary.builder()
                        .type(req.getType())
                        .subType(req.getSubType())
                        .rfisc(req.getRfisc())
                        .name(req.getName())
                        .description(req.getDescription())
                        .metadata(req.getMetadata())
                        .displayOrder(req.getDisplayOrder())
                        .airlineId(req.getAirlineId())
                        .build())
                .toList();

        List<AncillaryResponse> created = ancillaryRepository.saveAll(toInsert).stream()
                .map(saved -> AncillaryMapper.toResponse(saved, List.of()))
                .collect(Collectors.toList());

        return AncillaryBulkCreateResponse.builder()
                .created(created)
                .skipped(skipped)
                .build();
    }

    @Override
    public AncillaryResponse getById(Long id) {
        Ancillary ancillary = ancillaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.ANCILLARY_NOT_FOUND_BY_ID, id)));

        List<InsuranceCoverage> insuranceCoverages = insuranceCoverageRepository.findByAncillary(ancillary);
        List<InsuranceCoverageResponse> coverageResponseList = insuranceCoverages.stream()
                .map(InsuranceCoverageMapper::toResponse)
                .toList();

        return AncillaryMapper.toResponse(ancillary, coverageResponseList);
    }

    @Override
    public List<AncillaryResponse> getAllByAirlineId(Long userId, Long airlineId) {
        airlineIntegrationService.requireMembership(userId, airlineId);
        return ancillaryRepository.findByAirlineId(airlineId)
                .stream()
                .map(ancillary -> {
                    List<InsuranceCoverage> insuranceCoverages = insuranceCoverageRepository
                            .findByAncillary(ancillary);
                    List<InsuranceCoverageResponse> coverageResponseList = insuranceCoverages.stream()
                            .map(InsuranceCoverageMapper::toResponse)
                            .toList();
                    return AncillaryMapper.toResponse(ancillary, coverageResponseList);
                })
                .collect(Collectors.toList());
    }

    @Override
    public AncillaryResponse update(Long userId, Long id, AncillaryRequest request) {
        Ancillary ancillary = ancillaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.ANCILLARY_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, ancillary.getAirlineId(), AncillaryPermissions.MANAGE);

        ancillary.setType(request.getType());
        ancillary.setSubType(request.getSubType());
        ancillary.setRfisc(request.getRfisc());
        ancillary.setName(request.getName());
        ancillary.setDescription(request.getDescription());
        ancillary.setMetadata(request.getMetadata());
        ancillary.setDisplayOrder(request.getDisplayOrder());

        Ancillary updated = ancillaryRepository.save(ancillary);

        List<InsuranceCoverage> insuranceCoverages = insuranceCoverageRepository.findByAncillary(ancillary);
        List<InsuranceCoverageResponse> coverageResponseList = insuranceCoverages.stream()
                .map(InsuranceCoverageMapper::toResponse)
                .toList();

        return AncillaryMapper.toResponse(updated, coverageResponseList);
    }

    @Override
    public void delete(Long userId, Long id) {
        Ancillary ancillary = ancillaryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.ANCILLARY_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, ancillary.getAirlineId(), AncillaryPermissions.MANAGE);
        ancillaryRepository.delete(ancillary);
    }
}
