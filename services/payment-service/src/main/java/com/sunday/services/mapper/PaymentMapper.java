package com.sunday.services.mapper;

import com.sunday.common_lib.payload.response.PaymentDTO;
import com.sunday.services.model.Payment;

import java.time.ZoneId;

public class PaymentMapper {

    public static PaymentDTO toDTO(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setGateway(payment.getProvider());
        dto.setAmount(payment.getAmount());
        dto.setCurrency(payment.getCurrency());
        dto.setTransactionId(payment.getTransactionId());
        dto.setPaymentMethod(payment.getMethod());
        dto.setStatus(payment.getStatus());
        dto.setUserId(payment.getUserId());
        dto.setBookingId(payment.getBookingId());
        dto.setAirlineId(payment.getAirlineId());
        dto.setRefundId(payment.getRefundId());

        if (payment.getPaidAt() != null) {
            dto.setCompletedAt(payment.getPaidAt()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        if (payment.getCreatedAt() != null) {
            dto.setCreatedAt(payment.getCreatedAt()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
            dto.setInitiatedAt(payment.getCreatedAt()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        if (payment.getUpdatedAt() != null) {
            dto.setUpdatedAt(payment.getUpdatedAt()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime());
        }

        return dto;
    }
}
