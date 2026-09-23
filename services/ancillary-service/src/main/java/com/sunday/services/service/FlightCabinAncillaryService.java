package com.sunday.services.service;

import com.sunday.common_lib.enums.AncillaryType;
import com.sunday.common_lib.payload.request.FlightCabinAncillaryRequest;
import com.sunday.common_lib.payload.response.FlightCabinAncillaryBulkCreateResponse;
import com.sunday.common_lib.payload.response.FlightCabinAncillaryResponse;

import java.util.List;

public interface FlightCabinAncillaryService {

    FlightCabinAncillaryResponse create(Long userId, FlightCabinAncillaryRequest request);

    FlightCabinAncillaryBulkCreateResponse bulkCreate(Long userId, List<FlightCabinAncillaryRequest> requests);

    FlightCabinAncillaryResponse getById(Long id);

    List<FlightCabinAncillaryResponse> getAllByFlightAndCabinClass(
            Long flightId, Long cabinClassId);

    List<FlightCabinAncillaryResponse> getAllByIds(List<Long> ids);

    FlightCabinAncillaryResponse getByFlightIdAndCabinClassAndType(
            Long flightId, Long cabinClassId, AncillaryType type);

    List<FlightCabinAncillaryResponse> getAllByFlightIdAndCabinClassAndType(
            Long flightId, Long cabinClassId, AncillaryType type);

    FlightCabinAncillaryResponse update(Long userId, Long id, FlightCabinAncillaryRequest request);

    void delete(Long userId, Long id);

    Double calculateAncillaryPrice(List<Long> ancillaryIds);
}
