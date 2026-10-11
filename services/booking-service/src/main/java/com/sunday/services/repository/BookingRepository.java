package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.services.model.Booking;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>, BookingPerformanceRepository {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserId(UUID userId);

    long countByFlightInstanceIdAndAirlineId(UUID flightInstanceId, UUID airlineId);

    long countByAirlineId(UUID airlineId);

    long countByAirlineIdAndBookingDateBetween(UUID airlineId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b " +
            "WHERE b.airlineId = :airlineId AND b.bookingDate BETWEEN :from AND :to " +
            "AND b.status = BookingStatus.CONFIRMED")
    BigDecimal sumTotalAmountByAirlineIdAndBookingDateBetween(
            @Param("airlineId") UUID airlineId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b " +
            "WHERE b.airlineId = :airlineId AND b.status = BookingStatus.CONFIRMED")
    BigDecimal sumTotalAmountByAirlineId(@Param("airlineId") UUID airlineId);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.passengers " +
            "LEFT JOIN FETCH b.tickets " +
            "WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.passengers p " +
            "WHERE b.airlineId = :airlineId " +
            "AND (:search IS NULL OR " +
            "LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.contactInfo.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.contactInfo.phone) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR b.status = :status) " +
            "AND (:flightInstanceId IS NULL OR b.flightInstanceId = :flightInstanceId)")
    List<Booking> findByAirlineWithFilters(
            @Param("airlineId") UUID airlineId,
            @Param("search") String search,
            @Param("status") BookingStatus status,
            @Param("flightInstanceId") UUID flightInstanceId,
            Sort sort);
}
