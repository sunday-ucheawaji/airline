package com.sunday.services.repository;

import com.sunday.common_lib.enums.CoverageType;
import com.sunday.services.model.Ancillary;
import com.sunday.services.model.InsuranceCoverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceCoverageRepository extends JpaRepository<InsuranceCoverage, Long> {

    List<InsuranceCoverage> findByAncillary(Ancillary ancillary);

    List<InsuranceCoverage> findByAncillaryAndActiveTrue(Ancillary ancillary);

    List<InsuranceCoverage> findByCoverageType(CoverageType coverageType);

    List<InsuranceCoverage> findByAncillaryIdAndActiveTrue(Long ancillaryId);
}
