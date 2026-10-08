package com.sunday.services.repository;

import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.services.model.SeatInstance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SeatInstanceRepository extends JpaRepository<SeatInstance, Long> {

    List<SeatInstance> findByFlightId(Long flightId);
    List<SeatInstance> findByFlightInstanceId(Long flightInstanceId);
    List<SeatInstance> findByFlightInstanceCabinId(Long id);
    boolean existsBySeatIdAndFlightInstanceId(Long seatId, Long flightInstanceId);

    @Query("SELECT si FROM SeatInstance si WHERE si.flightId = :flightId AND si.status = 'AVAILABLE'")
    List<SeatInstance> findAvailableByFlightId(@Param("flightId") Long flightId);

    @Query("SELECT COUNT(si) FROM SeatInstance si WHERE si.flightId = :flightId AND si.status = 'AVAILABLE'")
    Long countAvailableByFlightId(@Param("flightId") Long flightId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM SeatInstance si JOIN FETCH si.seat s JOIN FETCH si.flightInstanceCabin fc WHERE si.id = :seatInstanceId")
    Optional<SeatInstance> findByIdForUpdate(@Param("seatInstanceId") Long seatInstanceId);

    /**
     * Locks every requested row in one query, for an atomic check-then-set across a whole booking's seats.
     * flightInstanceCabin/cabinClass are left-joined because the FK is nullable — an inner join would silently
     * drop such a row from the result instead of correctly failing it as "wrong cabin class".
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT si FROM SeatInstance si " +
            "JOIN FETCH si.seat s " +
            "LEFT JOIN FETCH si.flightInstanceCabin fc " +
            "LEFT JOIN FETCH fc.cabinClass cc " +
            "WHERE si.id IN :ids " +
            "ORDER BY si.id ASC")
    List<SeatInstance> findAllByIdForUpdate(@Param("ids") List<Long> ids);

    List<SeatInstance> findByStatusAndHeldUntilBefore(SeatAvailabilityStatus status, Instant time);
}
