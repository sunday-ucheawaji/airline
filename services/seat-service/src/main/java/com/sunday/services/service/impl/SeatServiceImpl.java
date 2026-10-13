package com.sunday.services.service.impl;

import com.sunday.common_lib.exception.BadRequestException;
import java.util.UUID;
import com.sunday.common_lib.constants.SeatPermissions;
import com.sunday.common_lib.enums.SeatType;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.SeatRequest;
import com.sunday.common_lib.payload.response.SeatResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.integration.AirlineIntegrationService;
import com.sunday.services.mapper.SeatMapper;
import com.sunday.services.model.CabinClass;
import com.sunday.services.model.Seat;
import com.sunday.services.model.SeatMap;
import com.sunday.services.repository.CabinClassRepository;
import com.sunday.services.repository.SeatMapRepository;
import com.sunday.services.repository.SeatRepository;
import com.sunday.services.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapRepository seatMapRepository;
    private final CabinClassRepository cabinClassRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    public void generateSeats(Collection<SeatMap> seatMaps) {
        List<Seat> seats = new ArrayList<>();
        for (SeatMap seatMap : seatMaps) {
            seats.addAll(buildSeats(seatMap));
        }
        seatRepository.saveAll(seats);
    }

    @Override
    public void regenerateSeats(SeatMap seatMap) {
        if (seatMap.getSeats() != null) {
            seatRepository.deleteAll(seatMap.getSeats());
            seatMap.getSeats().clear();
            seatRepository.flush();
        }
        seatRepository.saveAll(buildSeats(seatMap));
    }

    private List<Seat> buildSeats(SeatMap seatMap) {
        int leftSeatsPerRow = seatMap.getLeftSeatsPerRow();
        int rightSeatsPerRow = seatMap.getRightSeatsPerRow();
        int rows = seatMap.getTotalRows();
        int seatsPerRow = leftSeatsPerRow + rightSeatsPerRow;

        List<Seat> seats = new ArrayList<>();

        for (int row = 1; row <= rows; row++) {
            for (int col = 0; col < seatsPerRow; col++) {
                String seatNum = row + getSeatLetter(col);
                SeatType type = getSeatType(col, leftSeatsPerRow, rightSeatsPerRow);

                seats.add(Seat.builder()
                        .seatNumber(seatNum)
                        .seatRow(row)
                        .columnLetter(getSeatLetter(col).charAt(0))
                        .seatType(type)
                        .seatMap(seatMap)
                        .cabinClass(seatMap.getCabinClass())
                        .build());
            }
        }

        if (seatMap.getSeats() == null) {
            seatMap.setSeats(new ArrayList<>());
        }
        seatMap.getSeats().addAll(seats);
        return seats;
    }

    private String getSeatLetter(int col) {
        StringBuilder sb = new StringBuilder();
        while (col >= 0) {
            sb.insert(0, (char) ('A' + (col % 26)));
            col = col / 26 - 1;
        }
        return sb.toString();
    }

    private SeatType getSeatType(int seatIndex, int leftBlockSeats, int rightBlockSeats) {
        int totalSeats = leftBlockSeats + rightBlockSeats;

        if (seatIndex == 0 || seatIndex == totalSeats - 1) return SeatType.WINDOW;
        if (seatIndex == leftBlockSeats - 1) return SeatType.AISLE;
        if (seatIndex == leftBlockSeats) return SeatType.AISLE;
        return SeatType.MIDDLE;
    }

    @Override
    @Transactional(readOnly = true)
    public SeatResponse getSeatById(UUID userId, UUID id) {
        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(userId, seat.getSeatMap().getAirlineId(), SeatPermissions.SEAT_READ);
        return SeatMapper.toResponse(seat);
    }

    @Override
    public SeatResponse updateSeat(UUID userId, UUID id, SeatRequest request) {
        Seat existing = seatRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessageUtil.SEAT_NOT_FOUND_BY_ID, id)));

        UUID currentAirlineId = existing.getSeatMap().getAirlineId();
        airlineIntegrationService.requirePermission(userId, currentAirlineId, SeatPermissions.SEAT_CONFIG_MANAGE);

        SeatMap seatMap = seatMapRepository.findById(request.getSeatMapId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.SEAT_MAP_NOT_FOUND_BY_ID, request.getSeatMapId())));

        if (!seatMap.getId().equals(existing.getSeatMap().getId())) {
            airlineIntegrationService.requirePermission(userId, seatMap.getAirlineId(), SeatPermissions.SEAT_CONFIG_MANAGE);
        }

        CabinClass cabinClass = null;
        if (request.getCabinClassId() != null) {
            cabinClass = cabinClassRepository.findById(request.getCabinClassId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ErrorMessageUtil.CABIN_CLASS_NOT_FOUND_BY_ID, request.getCabinClassId())));
            if (seatMap.getCabinClass() == null
                    || !Objects.equals(cabinClass.getAircraftId(), seatMap.getCabinClass().getAircraftId())) {
                throw new BadRequestException(String.format(
                        ErrorMessageUtil.SEAT_CABIN_CLASS_OTHER_AIRCRAFT, cabinClass.getId(), seatMap.getId()));
            }
        }

        SeatMapper.updateEntity(request, existing, seatMap, cabinClass);
        Seat saved = seatRepository.save(existing);
        return SeatMapper.toResponse(saved);
    }
}
