package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.SeatMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeatMapRepository extends JpaRepository<SeatMap, UUID> {

    SeatMap findByCabinClassId(UUID cabinClassId);
    List<SeatMap> findByCabinClassIdIn(Collection<UUID> cabinClassIds);
    boolean existsByAirlineIdAndCabinClassIdAndName(UUID airlineId, UUID cabinClassId, String name);
    boolean existsByAirlineIdAndNameAndIdNot(UUID airlineId, String name, UUID id);

    @Query("SELECT sm FROM SeatMap sm LEFT JOIN FETCH sm.cabinClass WHERE sm.id = :id")
    Optional<SeatMap> findByIdWithDetails(@Param("id") UUID id);

}
