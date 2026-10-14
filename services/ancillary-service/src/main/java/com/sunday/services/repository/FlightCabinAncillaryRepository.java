package com.sunday.services.repository;

import com.sunday.common_lib.enums.CabinClassType;
import java.util.UUID;
import com.sunday.common_lib.enums.AncillaryType;
import com.sunday.services.model.FlightCabinAncillary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlightCabinAncillaryRepository extends JpaRepository<FlightCabinAncillary, UUID> {

    List<FlightCabinAncillary> findByFlightId(UUID flightId);

    List<FlightCabinAncillary> findByFlightIdAndCabinClass(UUID flightId, CabinClassType cabinClass);

    List<FlightCabinAncillary> findByCabinClass(CabinClassType cabinClass);

    Optional<FlightCabinAncillary> findByFlightIdAndCabinClassAndAncillary_Type(
            UUID flightId, CabinClassType cabinClass, AncillaryType type);

    List<FlightCabinAncillary> findAllByFlightIdAndCabinClassAndAncillary_Type(
            UUID flightId, CabinClassType cabinClass, AncillaryType type);

    boolean existsByFlightIdAndCabinClassAndAncillary_Id(UUID flightId, CabinClassType cabinClass, UUID ancillaryId);

    /**
     * Over-fetches: a safe superset of rows whose flightId/cabinClass/ancillaryId each appear
     * somewhere in the batch, not necessarily the same combination. Callers must re-check the exact
     * (flightId, cabinClass, ancillaryId) triple in memory — same pattern as
     * {@code MealRepository.findByAirlineIdInAndCodeIn}.
     */
    List<FlightCabinAncillary> findByFlightIdInAndCabinClassInAndAncillary_IdIn(
            List<UUID> flightIds, List<CabinClassType> cabinClasses, List<UUID> ancillaryIds);
}
