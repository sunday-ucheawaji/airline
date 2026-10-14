package com.sunday.services.repository;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.services.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    String AIRLINE_FILTER = "WHERE b.airlineId = :airlineId " +
            "AND (:search IS NULL OR " +
            "LOWER(b.bookingReference) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.contactInfo.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(b.contactInfo.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "EXISTS (SELECT p FROM Passenger p WHERE p.booking = b AND (" +
            "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.email) LIKE LOWER(CONCAT('%', :search, '%'))))) " +
            "AND (:status IS NULL OR b.status = :status) " +
            "AND (:flightInstanceId IS NULL OR b.flightInstanceId = :flightInstanceId)";

    Optional<Booking> findByBookingReference(String bookingReference);

    boolean existsByBookingReference(String bookingReference);

    List<Booking> findByTripId(UUID tripId);

    boolean existsByExchangedFromBookingIdAndStatus(UUID exchangedFromBookingId, BookingStatus status);

    @Query("SELECT b.id FROM Booking b WHERE b.userId = :userId")
    Page<UUID> findIdsByUserId(@Param("userId") UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"passengers", "tickets"})
    List<Booking> findWithDetailsByIdIn(Collection<UUID> ids);

    long countByFlightInstanceIdAndAirlineId(UUID flightInstanceId, UUID airlineId);

    long countByAirlineIdAndBookingDateBetween(UUID airlineId, LocalDateTime from, LocalDateTime to);

    /** Sold bookings and their revenue per booking day and currency, from {@code from} on: one row per (day, currency). */
    @Query("SELECT cast(b.bookingDate as LocalDate) AS bookingDay, b.fareCurrency AS currency, " +
            "COUNT(b) AS bookings, SUM(b.totalAmount) AS total FROM Booking b " +
            "WHERE b.airlineId = :airlineId AND b.bookingDate >= :from " +
            "AND b.status IN (BookingStatus.CONFIRMED, BookingStatus.COMPLETED) " +
            "GROUP BY cast(b.bookingDate as LocalDate), b.fareCurrency")
    List<DailyTotal> sumSoldBookingsByDayAndCurrency(@Param("airlineId") UUID airlineId, @Param("from") LocalDateTime from);

    /** Confirmed bookings whose departure date has passed become COMPLETED. */
    @Modifying
    @Query("UPDATE Booking b SET b.status = BookingStatus.COMPLETED, b.version = b.version + 1 " +
            "WHERE b.status = BookingStatus.CONFIRMED AND b.fareTravelDate < :today")
    int completeDepartedBefore(@Param("today") LocalDate today);

    @Query("SELECT b.fareCurrency AS currency, SUM(b.totalAmount) AS total FROM Booking b " +
            "WHERE b.airlineId = :airlineId AND b.bookingDate BETWEEN :from AND :to " +
            "AND b.status IN (BookingStatus.CONFIRMED, BookingStatus.COMPLETED) GROUP BY b.fareCurrency")
    List<CurrencyTotal> sumSoldRevenueByCurrency(
            @Param("airlineId") UUID airlineId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.passengers " +
            "LEFT JOIN FETCH b.tickets " +
            "WHERE b.id = :id")
    Optional<Booking> findByIdWithDetails(@Param("id") UUID id);

    @Query(value = "SELECT b.id FROM Booking b " + AIRLINE_FILTER, countQuery = "SELECT COUNT(b) FROM Booking b " + AIRLINE_FILTER)
    Page<UUID> findIdsByAirlineWithFilters(
            @Param("airlineId") UUID airlineId,
            @Param("search") String search,
            @Param("status") BookingStatus status,
            @Param("flightInstanceId") UUID flightInstanceId,
            Pageable pageable);
}
