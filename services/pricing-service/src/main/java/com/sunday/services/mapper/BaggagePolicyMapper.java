package com.sunday.services.mapper;

import java.util.UUID;
import com.sunday.common_lib.payload.request.BaggagePolicyRequest;
import com.sunday.common_lib.payload.response.BaggagePolicyResponse;
import com.sunday.services.model.BaggagePolicy;
import com.sunday.services.model.Fare;

public class BaggagePolicyMapper {

    public static BaggagePolicy toEntity(BaggagePolicyRequest request, Fare fare, UUID userId) {
        if (request == null) return null;
        return BaggagePolicy.builder()
                .fare(fare)
                .airlineId(fare.getAirlineId())
                .createdBy(userId)
                .updatedBy(userId)
                .name(request.getName())
                .description(request.getDescription())
                .cabinBaggageMaxWeight(request.getCabinBaggageMaxWeight())
                .cabinBaggagePieces(request.getCabinBaggagePieces() != null ? request.getCabinBaggagePieces() : 1)
                .cabinBaggageWeightPerPiece(request.getCabinBaggageWeightPerPiece())
                .cabinBaggageMaxDimension(request.getCabinBaggageMaxDimension())
                .checkInBaggageMaxWeight(request.getCheckInBaggageMaxWeight())
                .checkInBaggagePieces(request.getCheckInBaggagePieces() != null ? request.getCheckInBaggagePieces() : 1)
                .checkInBaggageWeightPerPiece(request.getCheckInBaggageWeightPerPiece())
                .freeCheckedBagsAllowance(request.getFreeCheckedBagsAllowance() != null ? request.getFreeCheckedBagsAllowance() : 0)
                .priorityBaggage(request.getPriorityBaggage() != null ? request.getPriorityBaggage() : false)
                .extraBaggageAllowance(request.getExtraBaggageAllowance() != null ? request.getExtraBaggageAllowance() : false)
                .build();
    }

    public static BaggagePolicyResponse toResponse(BaggagePolicy policy) {
        if (policy == null) return null;
        return BaggagePolicyResponse.builder()
                .id(policy.getId())
                .name(policy.getName())
                .description(policy.getDescription())
                .cabinBaggageMaxWeight(policy.getCabinBaggageMaxWeight())
                .cabinBaggagePieces(policy.getCabinBaggagePieces())
                .cabinBaggageWeightPerPiece(policy.getCabinBaggageWeightPerPiece())
                .cabinBaggageMaxDimension(policy.getCabinBaggageMaxDimension())
                .checkInBaggageMaxWeight(policy.getCheckInBaggageMaxWeight())
                .checkInBaggagePieces(policy.getCheckInBaggagePieces())
                .checkInBaggageWeightPerPiece(policy.getCheckInBaggageWeightPerPiece())
                .freeCheckedBagsAllowance(policy.getFreeCheckedBagsAllowance())
                .priorityBaggage(policy.getPriorityBaggage())
                .extraBaggageAllowance(policy.getExtraBaggageAllowance())
                .airlineId(policy.getAirlineId())
                .fareId(policy.getFare() != null ? policy.getFare().getId() : null)
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    /** Full replace: a field left out of the request is cleared or reset to its create default. */
    public static void updateEntity(BaggagePolicyRequest request, BaggagePolicy existing, UUID userId) {
        if (request == null || existing == null) return;
        existing.setUpdatedBy(userId);
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setCabinBaggageMaxWeight(request.getCabinBaggageMaxWeight());
        existing.setCabinBaggagePieces(request.getCabinBaggagePieces() != null ? request.getCabinBaggagePieces() : 1);
        existing.setCabinBaggageWeightPerPiece(request.getCabinBaggageWeightPerPiece());
        existing.setCabinBaggageMaxDimension(request.getCabinBaggageMaxDimension());
        existing.setCheckInBaggageMaxWeight(request.getCheckInBaggageMaxWeight());
        existing.setCheckInBaggagePieces(request.getCheckInBaggagePieces() != null ? request.getCheckInBaggagePieces() : 1);
        existing.setCheckInBaggageWeightPerPiece(request.getCheckInBaggageWeightPerPiece());
        existing.setFreeCheckedBagsAllowance(request.getFreeCheckedBagsAllowance() != null ? request.getFreeCheckedBagsAllowance() : 0);
        existing.setPriorityBaggage(request.getPriorityBaggage() != null ? request.getPriorityBaggage() : false);
        existing.setExtraBaggageAllowance(request.getExtraBaggageAllowance() != null ? request.getExtraBaggageAllowance() : false);
    }
}
