package com.sunday.services.mapper;

import java.util.UUID;
import com.sunday.common_lib.payload.request.FareRulesRequest;
import com.sunday.common_lib.payload.response.FareRulesResponse;
import com.sunday.services.model.Fare;
import com.sunday.services.model.FareRules;

public class FareRulesMapper {

    public static FareRules toEntity(FareRulesRequest request, Fare fare, UUID userId) {
        if (request == null) return null;
        return FareRules.builder()
                .ruleName(request.getRuleName())
                .fare(fare)
                .airlineId(fare.getAirlineId())
                .createdBy(userId)
                .updatedBy(userId)
                .isRefundable(request.getIsRefundable())
                .changeFee(request.getChangeFee())
                .cancellationFee(request.getCancellationFee())
                .refundDeadlineDays(request.getRefundDeadlineDays())
                .changeDeadlineHours(request.getChangeDeadlineHours())
                .isChangeable(request.getIsChangeable() != null ? request.getIsChangeable() : false)
                .build();
    }

    public static FareRulesResponse toResponse(FareRules fareRules) {
        if (fareRules == null) return null;
        return FareRulesResponse.builder()
                .id(fareRules.getId())
                .ruleName(fareRules.getRuleName())
                .fareId(fareRules.getFare() != null ? fareRules.getFare().getId() : null)
                .airlineId(fareRules.getAirlineId())
                .isRefundable(fareRules.getIsRefundable())
                .changeFee(fareRules.getChangeFee())
                .cancellationFee(fareRules.getCancellationFee())
                .refundDeadlineDays(fareRules.getRefundDeadlineDays())
                .changeDeadlineHours(fareRules.getChangeDeadlineHours())
                .isChangeable(fareRules.getIsChangeable())
                .createdAt(fareRules.getCreatedAt())
                .updatedAt(fareRules.getUpdatedAt())
                .build();
    }

    /** Full replace: a field left out of the request is cleared, as on create. The fare is never changed. */
    public static void updateEntity(FareRulesRequest request, FareRules existing, UUID userId) {
        if (request == null || existing == null) return;
        existing.setRuleName(request.getRuleName());
        existing.setUpdatedBy(userId);
        existing.setIsRefundable(request.getIsRefundable());
        existing.setChangeFee(request.getChangeFee());
        existing.setCancellationFee(request.getCancellationFee());
        existing.setRefundDeadlineDays(request.getRefundDeadlineDays());
        existing.setChangeDeadlineHours(request.getChangeDeadlineHours());
        existing.setIsChangeable(request.getIsChangeable() != null ? request.getIsChangeable() : false);
    }
}
