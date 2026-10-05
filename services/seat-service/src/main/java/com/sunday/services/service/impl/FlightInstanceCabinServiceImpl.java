package com.sunday.services.service.impl;

import com.sunday.common_lib.constants.SeatPermissions;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
import com.sunday.common_lib.enums.SeatType;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.payload.request.FlightInstanceCabinRequest;
import com.sunday.common_lib.payload.response.FlightInstanceCabinResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.integration.AirlineIntegrationService;
import com.sunday.services.mapper.FlightInstanceCabinMapper;
import com.sunday.services.model.CabinClass;
import com.sunday.services.model.FlightInstanceCabin;
import com.sunday.services.model.SeatInstance;
import com.sunday.services.model.SeatMap;
import com.sunday.services.repository.*;
import com.sunday.services.service.FlightInstanceCabinService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FlightInstanceCabinServiceImpl implements FlightInstanceCabinService {

    private final FlightInstanceCabinRepository flightInstanceCabinRepository;
    private final CabinClassRepository cabinClassRepository;
    private final SeatMapRepository seatMapRepository;
    private final SeatInstanceRepository seatInstanceRepository;
    private final AirlineIntegrationService airlineIntegrationService;

    @Override
    public FlightInstanceCabinResponse createFlightInstanceCabin(Long userId, FlightInstanceCabinRequest request) {
        CabinClass cabinClass = cabinClassRepository.findById(request.getCabinClassId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.CABIN_CLASS_NOT_FOUND_BY_ID, request.getCabinClassId())));

        SeatMap seatMap = seatMapRepository.findByCabinClassId(cabinClass.getId());
        if (seatMap == null) {
            throw new ResourceNotFoundException(
                    String.format(ErrorMessageUtil.SEAT_MAP_NOT_FOUND_BY_CABIN_CLASS_ID, cabinClass.getId()));
        }

        airlineIntegrationService.requirePermission(userId, seatMap.getAirlineId(), SeatPermissions.SEAT_INSTANCE_CONFIG_MANAGE);

        if (seatMap.getSeats() == null || seatMap.getSeats().isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format(ErrorMessageUtil.NO_SEATS_IN_SEAT_MAP, cabinClass.getId()));
        }
        int totalSeats = seatMap.getSeats().size();

        FlightInstanceCabin fic = FlightInstanceCabin.builder()
                .flightInstanceId(request.getFlightInstanceId())
                .cabinClass(cabinClass)
                .totalSeats(totalSeats)
                .build();

        FlightInstanceCabin savedFlightInstanceCabin = flightInstanceCabinRepository.save(fic);

        List<SeatInstance> seatInstances = seatMap.getSeats().stream()
                .map(seat -> {
                    Double premiumSurcharge = getPremiumSurcharge(seat.getSeatType(), 1000.0, 500.0);
                    return SeatInstance.builder()
                            .flightId(request.getFlightId())
                            .status(SeatAvailabilityStatus.AVAILABLE)
                            .flightInstanceId(request.getFlightInstanceId())
                            .flightInstanceCabin(savedFlightInstanceCabin)
                            .seat(seat)
                            .premiumSurcharge(premiumSurcharge)
                            .build();
                })
                .toList();
        seatInstanceRepository.saveAll(seatInstances);
        savedFlightInstanceCabin.setSeats(seatInstances);

        return FlightInstanceCabinMapper.toResponse(savedFlightInstanceCabin);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightInstanceCabinResponse getFlightInstanceCabinById(Long userId, Long id) {
        FlightInstanceCabin fic = flightInstanceCabinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_CABIN_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, fic.getCabinClass().getSeatMap().getAirlineId(), SeatPermissions.SEAT_READ);
        return FlightInstanceCabinMapper.toResponse(fic);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlightInstanceCabinResponse> getByFlightInstanceId(Long userId, Long flightInstanceId, Pageable pageable) {
        Page<FlightInstanceCabin> page = flightInstanceCabinRepository.findByFlightInstanceId(flightInstanceId, pageable);
        if (page.hasContent()) {
            FlightInstanceCabin first = page.getContent().get(0);
            airlineIntegrationService.requirePermission(
                    userId, first.getCabinClass().getSeatMap().getAirlineId(), SeatPermissions.SEAT_READ);
        }
        return page.map(FlightInstanceCabinMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightInstanceCabinResponse getByFlightInstanceIdAndCabinClassId(Long userId, Long flightInstanceId, Long cabinClassId) {
        FlightInstanceCabin cabin = flightInstanceCabinRepository.findByFlightInstanceIdAndCabinClassId(
                flightInstanceId, cabinClassId);
        if (cabin == null) {
            throw new ResourceNotFoundException(String.format(
                    ErrorMessageUtil.FLIGHT_INSTANCE_CABIN_NOT_FOUND_FOR_FLIGHT_AND_CABIN_CLASS,
                    flightInstanceId, cabinClassId));
        }
        airlineIntegrationService.requirePermission(
                userId, cabin.getCabinClass().getSeatMap().getAirlineId(), SeatPermissions.SEAT_READ);
        return FlightInstanceCabinMapper.toResponse(cabin);
    }

    @Override
    public FlightInstanceCabinResponse updateFlightInstanceCabin(Long userId, Long id, FlightInstanceCabinRequest request) {
        FlightInstanceCabin existing = flightInstanceCabinRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_CABIN_NOT_FOUND_BY_ID, id)));

        airlineIntegrationService.requirePermission(
                userId, existing.getCabinClass().getSeatMap().getAirlineId(), SeatPermissions.SEAT_INSTANCE_CONFIG_MANAGE);

        if (request.getCabinClassId() != null && !request.getCabinClassId().equals(existing.getCabinClass().getId())) {
            CabinClass cabinClass = cabinClassRepository.findById(request.getCabinClassId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ErrorMessageUtil.CABIN_CLASS_NOT_FOUND_BY_ID, request.getCabinClassId())));
            SeatMap destinationSeatMap = seatMapRepository.findByCabinClassId(cabinClass.getId());
            if (destinationSeatMap != null) {
                airlineIntegrationService.requirePermission(
                        userId, destinationSeatMap.getAirlineId(), SeatPermissions.SEAT_INSTANCE_CONFIG_MANAGE);
            }
            existing.setCabinClass(cabinClass);
        }

        FlightInstanceCabin saved = flightInstanceCabinRepository.save(existing);
        return FlightInstanceCabinMapper.toResponse(saved);
    }

    @Override
    public void deleteFlightInstanceCabin(Long userId, Long id) {
        FlightInstanceCabin fic = flightInstanceCabinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_CABIN_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, fic.getCabinClass().getSeatMap().getAirlineId(), SeatPermissions.SEAT_INSTANCE_CONFIG_MANAGE);
        flightInstanceCabinRepository.delete(fic);
    }

    private Double getPremiumSurcharge(SeatType seatType, Double windowSurcharge, Double aisleSurcharge) {
        if (seatType == null) return 0.0;
        return switch (seatType) {
            case WINDOW -> windowSurcharge != null ? windowSurcharge : 0.0;
            case AISLE -> aisleSurcharge != null ? aisleSurcharge : 0.0;
            default -> 0.0;
        };
    }
}
