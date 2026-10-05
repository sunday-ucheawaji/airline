package com.sunday.services.repository;

import com.sunday.services.model.Seat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findBySeatMapId(Long seatMapId);
    List<Seat> findByCabinClassId(Long cabinClassId);

    Page<Seat> findBySeatMapId(Long seatMapId, Pageable pageable);
}
