package com.sunday.services.mapper;

import com.sunday.common_lib.payload.response.CustomerSeatResponse;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.payload.request.SeatInstanceRequest;
import com.sunday.common_lib.payload.response.SeatInstanceResponse;
import com.sunday.services.model.FlightInstanceCabin;
import com.sunday.services.model.Seat;
import com.sunday.services.model.SeatInstance;

public class SeatInstanceMapper {

    public static SeatInstance toEntity(SeatInstanceRequest request, Seat seat,
                                        FlightInstanceCabin flightInstanceCabin) {
        return SeatInstance.builder()
                .flightId(request.getFlightId())
                .seat(seat)
                .flightInstanceCabin(flightInstanceCabin)
                .flightInstanceId(request.getFlightInstanceId())
                .status(request.getStatus() != null ? request.getStatus() : SeatAvailabilityStatus.AVAILABLE)
                .mealPreference(request.getMealPreference())
                .fare(request.getFare())
                .flightScheduleId(request.getFlightScheduleId())
                .build();
    }

    public static CustomerSeatResponse toCustomerResponse(SeatInstance si) {
        Seat seat = si.getSeat();
        return CustomerSeatResponse.builder()
                .seatInstanceId(si.getId())
                .seatNumber(seat.getSeatNumber())
                .row(seat.getSeatRow())
                .column(seat.getColumnLetter())
                .seatType(seat.getSeatType())
                .cabinClass(si.getFlightInstanceCabin().getCabinClass().getName())
                .premiumSurcharge(si.getPremiumSurcharge())
                .available(si.getStatus() == SeatAvailabilityStatus.AVAILABLE)
                .emergencyExit(Boolean.TRUE.equals(seat.getIsEmergencyExit()))
                .extraLegroom(Boolean.TRUE.equals(seat.getHasExtraLegroom()))
                .bassinet(Boolean.TRUE.equals(seat.getHasBassinet()))
                .wheelchairAccessible(Boolean.TRUE.equals(seat.getIsWheelchairAccessible()))
                .powerOutlet(Boolean.TRUE.equals(seat.getHasPowerOutlet()))
                .nearLavatory(Boolean.TRUE.equals(seat.getIsNearLavatory()))
                .build();
    }

    public static SeatInstanceResponse toResponse(SeatInstance si) {
        return SeatInstanceResponse.builder()
                .id(si.getId())
                .flightId(si.getFlightId())
                .seatId(si.getSeat() != null ? si.getSeat().getId() : null)
                .seatNumber(si.getSeat() != null ? si.getSeat().getSeatNumber() : null)
                .seatType(si.getSeat() != null ? si.getSeat().getSeatType().name() : null)
                .seatPosition(si.getSeat() != null ? si.getSeat().getFullPosition() : null)
                .seat(SeatMapper.toResponse(si.getSeat()))
                .status(si.getStatus())
                .flightInstanceId(si.getFlightInstanceId())
                .flightCabinId(si.getFlightInstanceCabin() != null ? si.getFlightInstanceCabin().getId() : null)
                .mealPreference(si.getMealPreference())
                .fare(si.getFare())
                .price(si.getPremiumSurcharge())
                .version(si.getVersion())
                .createdAt(si.getCreatedAt())
                .updatedAt(si.getUpdatedAt())
                .isOccupied(si.getStatus() == SeatAvailabilityStatus.OCCUPIED)
                .seatCharacteristics(
                        si.getSeat() != null ? si.getSeat().getSeatCharacteristics() : null)
                .build();
    }
}
