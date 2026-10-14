package com.sunday.services.service;

import com.sunday.common_lib.enums.CabinClassType;
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
            UUID flightId, CabinClassType cabinClass);

    List<FlightCabinAncillaryResponse> getAllByIds(List<UUID> ids);

    FlightCabinAncillaryResponse getByFlightIdAndCabinClassAndType(
            UUID flightId, CabinClassType cabinClass, AncillaryType type);

    List<FlightCabinAncillaryResponse> getAllByFlightIdAndCabinClassAndType(
            UUID flightId, CabinClassType cabinClass, AncillaryType type);

    FlightCabinAncillaryResponse update(UUID userId, UUID id, FlightCabinAncillaryRequest request);

    void delete(UUID userId, UUID id);

    /**
     * Prices the offerings for one booking: each must be on this flight and cabin, and a repeated id means "buy N",
     * at most {@code maxQuantity} per passenger.
     */
    BigDecimal calculateAncillaryPrice(UUID flightId, CabinClassType cabinClass, int passengers, List<UUID> ancillaryIds);
}
