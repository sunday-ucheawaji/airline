package com.sunday.services.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import com.sunday.common_lib.enums.PaymentStatus;
import com.sunday.services.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByBookingId(UUID bookingId);

    /** A booking can have several attempts (failed, then retried), so the lookup is for the latest one. */
    Optional<Payment> findFirstByBookingIdOrderByIdDesc(UUID bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByBookingIdIn(Collection<UUID> bookingIds);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Page<Payment> findByAirlineId(UUID airlineId, Pageable pageable);

    /** Locks the payment row, so refunds of one payment run one at a time and each sees the others' totals. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> lockById(@Param("id") UUID id);
}
