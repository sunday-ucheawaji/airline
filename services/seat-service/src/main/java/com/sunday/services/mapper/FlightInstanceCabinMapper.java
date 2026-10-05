package com.sunday.services.mapper;

import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.payload.response.FlightInstanceCabinResponse;
import com.sunday.services.model.FlightInstanceCabin;

import java.util.List;
import java.util.stream.Collectors;

public class FlightInstanceCabinMapper {

    public static FlightInstanceCabinResponse toResponse(FlightInstanceCabin fic) {
        if (fic == null) return null;
        List<com.sunday.services.model.SeatInstance> seats = fic.getSeats();
        long bookedSeats = seats != null
                ? seats.stream().filter(si -> si.getStatus() != SeatAvailabilityStatus.AVAILABLE).count()
                : 0;
        long availableSeats = seats != null ? seats.size() - bookedSeats : 0;

        return FlightInstanceCabinResponse.builder()
                .id(fic.getId())
                .flightInstanceId(fic.getFlightInstanceId())
                .cabinClassType(fic.getCabinClass() != null ? fic.getCabinClass().getName() : null)
                .cabinClass(fic.getCabinClass() != null ?
                        CabinClassMapper.toResponse(fic.getCabinClass(), fic.getCabinClass().getSeatMap()) : null)
                .seats(seats != null ?
                        seats.stream().map(SeatInstanceMapper::toResponse)
                                .collect(Collectors.toList()) : null)
                .seatMap(fic.getCabinClass() != null && fic.getCabinClass().getSeatMap() != null ?
                        SeatMapMapper.toSimpleResponse(fic.getCabinClass().getSeatMap()) : null)
                .totalSeats(fic.getTotalSeats())
                .bookedSeats((int) bookedSeats)
                .availableSeats((int) availableSeats)
                .build();
    }
}
