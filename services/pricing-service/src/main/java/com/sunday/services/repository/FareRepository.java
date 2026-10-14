package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.enums.FareStatus;
import com.sunday.common_lib.enums.PassengerType;
import com.sunday.services.model.Fare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FareRepository extends JpaRepository<Fare, UUID> {

    @EntityGraph(attributePaths = {"fareRules", "baggagePolicy"})
    Page<Fare> findByAirlineId(UUID airlineId, Pageable pageable);

    @EntityGraph(attributePaths = {"fareRules", "baggagePolicy"})
    List<Fare> findByFlightIdAndCabinClass(UUID flightId, CabinClassType cabinClass);

    /** The fare filed alongside another under the same airline, flight, cabin, RBD and name, for another passenger type. */
    @EntityGraph(attributePaths = {"fareRules", "baggagePolicy"})
    Optional<Fare> findByAirlineIdAndFlightIdAndCabinClassAndRbdCodeAndPassengerTypeAndName(
            UUID airlineId, UUID flightId, CabinClassType cabinClass, Character rbdCode, PassengerType passengerType, String name);

    @EntityGraph(attributePaths = {"fareRules", "baggagePolicy"})
    List<Fare> findByIdIn(Collection<UUID> ids);

    boolean existsByAirlineIdAndFlightIdAndCabinClassAndRbdCodeAndPassengerTypeAndName(
            UUID airlineId, UUID flightId, CabinClassType cabinClass, Character rbdCode, PassengerType passengerType, String name);

    boolean existsByAirlineIdAndFlightIdAndCabinClassAndRbdCodeAndPassengerTypeAndNameAndIdNot(
            UUID airlineId, UUID flightId, CabinClassType cabinClass, Character rbdCode, PassengerType passengerType,
            String name, UUID id);

    /**
     * Search candidates for a page of flight departures, in one query. Keeps published fares of the cabin and
     * passenger type whose sales window contains {@code now} and whose travel window overlaps the requested dates. The per-departure
     * travel and advance-purchase rules are applied in memory by FareValidityPolicy. Rules and baggage are fetched
     * here, because the search response includes them and loading them per fare would query inside a loop.
     */
    @Query("""
        SELECT f FROM Fare f
        LEFT JOIN FETCH f.fareRules
        LEFT JOIN FETCH f.baggagePolicy
        WHERE f.flightId IN :flightIds
          AND f.cabinClass = :cabinClass
          AND f.passengerType = :passengerType
          AND f.status = :status
          AND f.salesFrom <= :now
          AND f.salesTo >= :now
          AND f.travelFrom <= :lastDate
          AND f.travelTo >= :firstDate
    """)
    List<Fare> findSearchCandidates(@Param("flightIds") Collection<UUID> flightIds,
                                    @Param("cabinClass") CabinClassType cabinClass,
                                    @Param("passengerType") PassengerType passengerType,
                                    @Param("status") FareStatus status,
                                    @Param("now") Instant now,
                                    @Param("firstDate") LocalDate firstDate,
                                    @Param("lastDate") LocalDate lastDate);

    /**
     * All fares on the given flights, in one query. Bulk create compares natural keys in memory against this,
     * so duplicate detection costs one database round trip per batch, not one per row.
     */
    @EntityGraph(attributePaths = {"fareRules", "baggagePolicy"})
    List<Fare> findByFlightIdIn(Collection<UUID> flightIds);
}
