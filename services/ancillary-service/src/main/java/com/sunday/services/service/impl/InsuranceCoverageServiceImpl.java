package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.InsuranceCoverageRequest;
import com.sunday.common_lib.payload.response.InsuranceCoverageResponse;
import com.sunday.services.mapper.InsuranceCoverageMapper;
import com.sunday.services.model.Ancillary;
import com.sunday.services.model.InsuranceCoverage;
import com.sunday.services.repository.AncillaryRepository;
import com.sunday.services.repository.InsuranceCoverageRepository;
import com.sunday.services.service.InsuranceCoverageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsuranceCoverageServiceImpl implements InsuranceCoverageService {

    private final InsuranceCoverageRepository coverageRepository;
    private final AncillaryRepository ancillaryRepository;

    @Override
    @Transactional
    public InsuranceCoverageResponse createCoverage(InsuranceCoverageRequest request)
            throws ResourceNotFoundException {
        Ancillary ancillary = ancillaryRepository.findById(request.getAncillaryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ancillary not found with ID: " + request.getAncillaryId()));

        InsuranceCoverage coverage = InsuranceCoverageMapper.toEntity(request, ancillary);
        InsuranceCoverage saved = coverageRepository.save(coverage);
        return InsuranceCoverageMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public List<InsuranceCoverageResponse> createCoveragesBulk(List<InsuranceCoverageRequest> requests)
            throws ResourceNotFoundException {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Coverage request list cannot be empty");
        }

        Long ancillaryId = requests.get(0).getAncillaryId();
        boolean allSameAncillary = requests.stream()
                .allMatch(req -> req.getAncillaryId().equals(ancillaryId));

        if (!allSameAncillary) {
            throw new IllegalArgumentException(
                    "All coverages in bulk request must belong to the same ancillary");
        }

        Ancillary ancillary = ancillaryRepository.findById(ancillaryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ancillary not found with ID: " + ancillaryId));

        List<InsuranceCoverage> coverages = requests.stream()
                .map(request -> InsuranceCoverageMapper.toEntity(request, ancillary))
                .collect(Collectors.toList());

        List<InsuranceCoverage> saved = coverageRepository.saveAll(coverages);
        return saved.stream()
                .map(InsuranceCoverageMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InsuranceCoverageResponse updateCoverage(Long id, InsuranceCoverageRequest request)
            throws ResourceNotFoundException {
        InsuranceCoverage existing = coverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Insurance coverage not found with ID: " + id));

        Ancillary ancillary = null;
        if (request.getAncillaryId() != null) {
            ancillary = ancillaryRepository.findById(request.getAncillaryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ancillary not found with ID: " + request.getAncillaryId()));
        }

        InsuranceCoverageMapper.updateEntityFromRequest(existing, request, ancillary);
        InsuranceCoverage updated = coverageRepository.save(existing);
        return InsuranceCoverageMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteCoverage(Long id) throws ResourceNotFoundException {
        InsuranceCoverage coverage = coverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Insurance coverage not found with ID: " + id));
        coverageRepository.delete(coverage);
    }

    @Override
    public InsuranceCoverageResponse getCoverageById(Long id) throws ResourceNotFoundException {
        InsuranceCoverage coverage = coverageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Insurance coverage not found with ID: " + id));
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
