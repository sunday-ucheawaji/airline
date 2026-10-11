package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.services.model.CabinClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CabinClassRepository extends JpaRepository<CabinClass, UUID> {

    boolean existsByCodeAndAircraftId(String code, UUID aircraftId);
    boolean existsByCodeAndAircraftIdAndIdNot(String code, UUID aircraftId, UUID id);
    List<CabinClass> findByAircraftId(UUID aircraftId);
    List<CabinClass> findByAircraftIdIn(Collection<UUID> aircraftIds);

    CabinClass findByAircraftIdAndName(UUID flightId, CabinClassType cabinClassType);
}
