package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.Seat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    List<Seat> findBySeatMapId(UUID seatMapId);
    List<Seat> findByCabinClassId(UUID cabinClassId);

    Page<Seat> findBySeatMapId(UUID seatMapId, Pageable pageable);
}
