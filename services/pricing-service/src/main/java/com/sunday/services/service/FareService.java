package com.sunday.services.service;


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

    FareResponse createFare(Long userId, FareRequest request);
    FareBulkCreateResponse createFares(Long userId, List<FareRequest> requests);
    FareResponse getFareById(Long userId, Long id);
    List<FareResponse> getFaresByFlightIdAndCabinClassId(Long userId, Long flightId, Long cabinClassId);
    Page<FareResponse> getFaresByAirline(Long userId, Long airlineId, Pageable pageable);
    FareResponse updateFare(Long userId, Long id, FareRequest request);
    void deleteFare(Long userId, Long id);

    FareResponse getFareByIdInternal(Long id);

    Map<Long, FareResponse> getFaresByIds(List<Long> ids);
}
