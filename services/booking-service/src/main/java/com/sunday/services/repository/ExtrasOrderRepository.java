package com.sunday.services.repository;

import com.sunday.common_lib.enums.BookingStatus;
import com.sunday.services.model.ExtrasOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExtrasOrderRepository extends JpaRepository<ExtrasOrder, UUID> {

    List<ExtrasOrder> findByBookingIdIn(Collection<UUID> bookingIds);

    List<ExtrasOrder> findByBookingIdAndStatusIn(UUID bookingId, Collection<BookingStatus> statuses);

    Optional<ExtrasOrder> findByBookingIdAndIdempotencyKey(UUID bookingId, String idempotencyKey);

    @Query("SELECT o.currency AS currency, SUM(o.totalAmount) AS total FROM ExtrasOrder o " +
            "WHERE o.airlineId = :airlineId AND o.createdAt BETWEEN :from AND :to " +
            "AND o.status = BookingStatus.CONFIRMED GROUP BY o.currency")
    List<CurrencyTotal> sumConfirmedRevenueByCurrency(@Param("airlineId") UUID airlineId,
                                                      @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Confirmed extras revenue per order day and currency, from {@code from} on; getBookings() is the order count. */
    @Query("SELECT cast(o.createdAt as LocalDate) AS bookingDay, o.currency AS currency, " +
            "COUNT(o) AS bookings, SUM(o.totalAmount) AS total FROM ExtrasOrder o " +
            "WHERE o.airlineId = :airlineId AND o.createdAt >= :from AND o.status = BookingStatus.CONFIRMED " +
            "GROUP BY cast(o.createdAt as LocalDate), o.currency")
    List<DailyTotal> sumConfirmedByDayAndCurrency(@Param("airlineId") UUID airlineId, @Param("from") LocalDateTime from);

    /** Orders never paid are cancelled after the payment timeout, so they stop counting against the extras limits. */
    @Modifying
    @Query("UPDATE ExtrasOrder o SET o.status = BookingStatus.CANCELLED, o.version = o.version + 1 " +
            "WHERE o.status = BookingStatus.PENDING AND o.createdAt < :before")
    int cancelUnpaidBefore(@Param("before") LocalDateTime before);
}
