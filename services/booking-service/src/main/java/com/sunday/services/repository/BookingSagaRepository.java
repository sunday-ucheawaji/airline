package com.sunday.services.repository;

import java.util.UUID;
import com.sunday.services.model.BookingSaga;
import com.sunday.services.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingSagaRepository extends JpaRepository<BookingSaga, UUID> {

    Optional<BookingSaga> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);

    Optional<BookingSaga> findByBookingId(UUID bookingId);

    List<BookingSaga> findByStatusInAndUpdatedAtBefore(List<SagaStatus> statuses, LocalDateTime cutoff);
}
