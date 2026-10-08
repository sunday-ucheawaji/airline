package com.sunday.services.repository;

import com.sunday.services.model.BookingSaga;
import com.sunday.services.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingSagaRepository extends JpaRepository<BookingSaga, Long> {

    Optional<BookingSaga> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<BookingSaga> findByBookingId(Long bookingId);

    List<BookingSaga> findByStatusInAndUpdatedAtBefore(List<SagaStatus> statuses, LocalDateTime cutoff);
}
