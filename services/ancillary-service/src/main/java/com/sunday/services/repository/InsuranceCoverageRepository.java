package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.common_lib.enums.CoverageType;
import com.sunday.services.model.Ancillary;
import com.sunday.services.model.InsuranceCoverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InsuranceCoverageRepository extends JpaRepository<InsuranceCoverage, UUID> {

    List<InsuranceCoverage> findByAncillary(Ancillary ancillary);

    /** Batch variant for mapping a list of ancillaries without one query per row. */
    List<InsuranceCoverage> findByAncillaryIn(Collection<Ancillary> ancillaries);

    List<InsuranceCoverage> findByAncillaryAndActiveTrue(Ancillary ancillary);

    List<InsuranceCoverage> findByCoverageType(CoverageType coverageType);

    List<InsuranceCoverage> findByAncillaryIdAndActiveTrue(UUID ancillaryId);
}
