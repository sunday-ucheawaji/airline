package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.common_lib.enums.AncillaryType;
import com.sunday.services.model.FlightCabinAncillary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlightCabinAncillaryRepository extends JpaRepository<FlightCabinAncillary, UUID> {

    List<FlightCabinAncillary> findByFlightId(UUID flightId);

    List<FlightCabinAncillary> findByFlightIdAndCabinClassId(UUID flightId, UUID cabinClassId);

    List<FlightCabinAncillary> findByCabinClassId(UUID cabinClassId);

    Optional<FlightCabinAncillary> findByFlightIdAndCabinClassIdAndAncillary_Type(
            UUID flightId, UUID cabinClassId, AncillaryType type);

    List<FlightCabinAncillary> findAllByFlightIdAndCabinClassIdAndAncillary_Type(
            UUID flightId, UUID cabinClassId, AncillaryType type);

    boolean existsByFlightIdAndCabinClassIdAndAncillary_Id(UUID flightId, UUID cabinClassId, UUID ancillaryId);

    /**
     * Over-fetches: a safe superset of rows whose flightId/cabinClassId/ancillaryId each appear
     * somewhere in the batch, not necessarily the same combination. Callers must re-check the exact
     * (flightId, cabinClassId, ancillaryId) triple in memory — same pattern as
     * {@code MealRepository.findByAirlineIdInAndCodeIn}.
     */
    List<FlightCabinAncillary> findByFlightIdInAndCabinClassIdInAndAncillary_IdIn(
            List<UUID> flightIds, List<UUID> cabinClassIds, List<UUID> ancillaryIds);
}
