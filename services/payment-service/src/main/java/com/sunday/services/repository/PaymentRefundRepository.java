package com.sunday.services.repository;

import com.sunday.services.model.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, UUID> {

    boolean existsByPaymentIdAndReference(UUID paymentId, String reference);
}
