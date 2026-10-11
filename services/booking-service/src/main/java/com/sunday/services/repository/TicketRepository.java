package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.common_lib.enums.TicketStatus;
import com.sunday.services.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByBookingId(UUID bookingId);

    List<Ticket> findByPassengerId(UUID passengerId);

    List<Ticket> findByStatus(TicketStatus status);

    boolean existsByTicketNumber(String ticketNumber);

    @Query("SELECT t FROM Ticket t " +
            "LEFT JOIN FETCH t.booking " +
            "LEFT JOIN FETCH t.passenger " +
            "WHERE t.booking.id = :bookingId")
    List<Ticket> findByBookingIdWithDetails(@Param("bookingId") UUID bookingId);
}
