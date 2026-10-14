package com.sunday.services.service;


import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.request.FareRequest;
import com.sunday.common_lib.payload.response.FareBulkCreateResponse;
import com.sunday.common_lib.payload.response.FareResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Methods taking a userId are permission-checked against the fare's own airline. The methods at the bottom
 * are service-to-service (booking, flight search) and carry no caller identity, so they are unchecked and
 * reachable only under /internal/fares/**.
 */
public interface FareService {

    FareResponse createFare(UUID userId, FareRequest request);
    FareBulkCreateResponse createFares(UUID userId, List<FareRequest> requests);
    FareResponse getFareById(UUID userId, UUID id);
    List<FareResponse> getFaresByFlightIdAndCabinClass(UUID userId, UUID flightId, CabinClassType cabinClass);
    Page<FareResponse> getFaresByAirline(UUID userId, UUID airlineId, Pageable pageable);
    FareResponse updateFare(UUID userId, UUID id, FareRequest request);
    void deleteFare(UUID userId, UUID id);

    FareResponse getFareByIdInternal(UUID id);

    Map<UUID, FareResponse> getFaresByIds(List<UUID> ids);
}
