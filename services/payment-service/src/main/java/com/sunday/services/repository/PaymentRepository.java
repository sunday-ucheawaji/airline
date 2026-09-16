package com.sunday.services.repository;

import com.sunday.common_lib.enums.PaymentStatus;
import com.sunday.services.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByBookingIdIn(Collection<Long> bookingIds);
}
