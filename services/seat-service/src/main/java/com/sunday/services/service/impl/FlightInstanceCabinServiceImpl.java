package com.sunday.services.service.impl;

import com.sunday.services.model.Seat;
import java.math.BigDecimal;
import com.sunday.services.integration.FlightInstanceGuard;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.BadRequestException;
import java.util.UUID;
import com.sunday.common_lib.constants.SeatPermissions;
import com.sunday.common_lib.enums.SeatAvailabilityStatus;
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
    private final FlightInstanceGuard flightInstanceGuard;

    @Override
    public FlightInstanceCabinResponse createFlightInstanceCabin(UUID userId, FlightInstanceCabinRequest request) {
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
        FlightInstanceResponse flightInstance = flightInstanceGuard.requireOperatedBy(request.getFlightInstanceId(), cabinClass);
        if (flightInstanceCabinRepository.existsByFlightInstanceIdAndCabinClassName(request.getFlightInstanceId(), cabinClass.getName())) {
            throw new ConflictException(String.format(
                    ErrorMessageUtil.FLIGHT_INSTANCE_CABIN_DUPLICATE, request.getFlightInstanceId(), cabinClass.getName()));
        }

        int totalSeats = seatMap.getSeats().size();
        FlightInstanceCabin fic = FlightInstanceCabin.builder()
                .flightInstanceId(request.getFlightInstanceId())
                .cabinClass(cabinClass)
                .totalSeats(totalSeats)
                .build();

        FlightInstanceCabin savedFlightInstanceCabin = flightInstanceCabinRepository.save(fic);

        List<SeatInstance> seatInstances = seatMap.getSeats().stream()
                .map(seat -> SeatInstance.builder()
                        .flightId(flightInstance.getFlightId())
                        .status(SeatAvailabilityStatus.AVAILABLE)
                        .flightInstanceId(request.getFlightInstanceId())
                        .flightInstanceCabin(savedFlightInstanceCabin)
                        .seat(seat)
                        .premiumSurcharge(surchargeFor(seat, request))
                        .build())
                .toList();
        seatInstanceRepository.saveAll(seatInstances);
        savedFlightInstanceCabin.setSeats(seatInstances);

        return FlightInstanceCabinMapper.toResponse(savedFlightInstanceCabin);
    }

    @Override
    @Transactional(readOnly = true)
    public FlightInstanceCabinResponse getFlightInstanceCabinById(UUID userId, UUID id) {
        FlightInstanceCabin fic = flightInstanceCabinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_CABIN_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, fic.getCabinClass().getSeatMap().getAirlineId(), SeatPermissions.SEAT_READ);
        return FlightInstanceCabinMapper.toResponse(fic);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlightInstanceCabinResponse> getByFlightInstanceId(UUID userId, UUID flightInstanceId, Pageable pageable) {
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
    public FlightInstanceCabinResponse getByFlightInstanceIdAndCabinClassId(UUID userId, UUID flightInstanceId, UUID cabinClassId) {
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
    public FlightInstanceCabinResponse updateFlightInstanceCabin(UUID userId, UUID id, FlightInstanceCabinRequest request) {
        FlightInstanceCabin existing = flightInstanceCabinRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_CABIN_NOT_FOUND_BY_ID, id)));

        airlineIntegrationService.requirePermission(
                userId, existing.getCabinClass().getSeatMap().getAirlineId(), SeatPermissions.SEAT_INSTANCE_CONFIG_MANAGE);

        if (request.getCabinClassId() != null && !request.getCabinClassId().equals(existing.getCabinClass().getId())) {
            throw new BadRequestException(ErrorMessageUtil.FLIGHT_INSTANCE_CABIN_CLASS_IMMUTABLE);
        }

        FlightInstanceCabin saved = flightInstanceCabinRepository.save(existing);
        return FlightInstanceCabinMapper.toResponse(saved);
    }

    @Override
    public void deleteFlightInstanceCabin(UUID userId, UUID id) {
        FlightInstanceCabin fic = flightInstanceCabinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessageUtil.FLIGHT_INSTANCE_CABIN_NOT_FOUND_BY_ID, id)));
        airlineIntegrationService.requirePermission(
                userId, fic.getCabinClass().getSeatMap().getAirlineId(), SeatPermissions.SEAT_INSTANCE_CONFIG_MANAGE);
        flightInstanceCabinRepository.delete(fic);
    }

    /** The request's window/aisle surcharge for this flight when given, otherwise the seat's own. */
    private static BigDecimal surchargeFor(Seat seat, FlightInstanceCabinRequest request) {
        if (seat.getSeatType() == null) {
            return seat.getPremiumSurcharge();
        }
        BigDecimal override = switch (seat.getSeatType()) {
            case WINDOW -> request.getWindowSurcharge();
            case AISLE -> request.getAisleSurcharge();
            default -> null;
        };
        return override != null ? override : seat.getPremiumSurcharge();
    }
}
