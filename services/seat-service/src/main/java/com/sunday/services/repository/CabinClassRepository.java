package com.sunday.services.repository;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.services.model.CabinClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CabinClassRepository extends JpaRepository<CabinClass, Long> {
    boolean existsByCode(String code);
    boolean existsByCodeAndAircraftId(String code, Long aircraftId);
    boolean existsByCodeAndAircraftIdAndIdNot(String code, Long aircraftId, Long id);
    List<CabinClass> findByAircraftId(Long aircraftId);

    CabinClass findByAircraftIdAndName(Long flightId, CabinClassType cabinClassType);
}
