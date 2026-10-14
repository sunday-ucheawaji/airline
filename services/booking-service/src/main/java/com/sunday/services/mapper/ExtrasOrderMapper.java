package com.sunday.services.mapper;

import com.sunday.common_lib.payload.response.ExtrasOrderResponse;
import com.sunday.common_lib.payload.response.PaymentInitiateResponse;
import com.sunday.services.model.ExtrasOrder;

import java.util.List;

public class ExtrasOrderMapper {

    public static ExtrasOrderResponse toResponse(ExtrasOrder order, PaymentInitiateResponse payment) {
        return ExtrasOrderResponse.builder()
                .id(order.getId())
                .bookingId(order.getBookingId())
                .status(order.getStatus())
                .currency(order.getCurrency())
                .totalAmount(order.getTotalAmount())
                .refundAmount(order.getRefundAmount())
                .paymentId(order.getPaymentId())
                .ancillaryIds(List.copyOf(order.getAncillaryIds()))
                .mealIds(List.copyOf(order.getMealIds()))
                .createdAt(order.getCreatedAt())
                .payment(payment)
                .build();
    }
}
