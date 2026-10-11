package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.enums.AncillaryType;
import com.sunday.common_lib.payload.request.FlightCabinAncillaryRequest;
import com.sunday.common_lib.payload.response.FlightCabinAncillaryBulkCreateResponse;
import com.sunday.common_lib.payload.response.FlightCabinAncillaryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface FlightCabinAncillaryService {

    FlightCabinAncillaryResponse create(UUID userId, FlightCabinAncillaryRequest request);

    FlightCabinAncillaryBulkCreateResponse bulkCreate(UUID userId, List<FlightCabinAncillaryRequest> requests);

    FlightCabinAncillaryResponse getById(UUID id);

    List<FlightCabinAncillaryResponse> getAllByFlightAndCabinClass(
            UUID flightId, UUID cabinClassId);

    List<FlightCabinAncillaryResponse> getAllByIds(List<UUID> ids);

    FlightCabinAncillaryResponse getByFlightIdAndCabinClassAndType(
            UUID flightId, UUID cabinClassId, AncillaryType type);

    List<FlightCabinAncillaryResponse> getAllByFlightIdAndCabinClassAndType(
            UUID flightId, UUID cabinClassId, AncillaryType type);

    FlightCabinAncillaryResponse update(UUID userId, UUID id, FlightCabinAncillaryRequest request);

    void delete(UUID userId, UUID id);

    BigDecimal calculateAncillaryPrice(UUID flightId, List<UUID> ancillaryIds);
}
