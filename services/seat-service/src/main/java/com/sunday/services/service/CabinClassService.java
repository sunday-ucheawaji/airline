package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.request.CabinClassRequest;
import com.sunday.common_lib.payload.response.CabinClassBulkCreateResponse;
import com.sunday.common_lib.payload.response.CabinClassResponse;

import java.util.List;

public interface CabinClassService {

    CabinClassResponse createCabinClass(UUID userId, CabinClassRequest request);
    CabinClassBulkCreateResponse createCabinClasses(UUID userId, List<CabinClassRequest> requests);
    CabinClassResponse getCabinClassById(UUID userId, UUID id);
    List<CabinClassResponse> getCabinClassesByAircraftId(UUID userId, UUID aircraftId);
    CabinClassResponse getByAircraftIdAndName(UUID userId, UUID aircraftId, CabinClassType name);
    CabinClassResponse updateCabinClass(UUID userId, UUID id, CabinClassRequest request);
    void deleteCabinClass(UUID userId, UUID id);
}
