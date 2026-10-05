package com.sunday.services.service;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.request.CabinClassRequest;
import com.sunday.common_lib.payload.response.CabinClassBulkCreateResponse;
import com.sunday.common_lib.payload.response.CabinClassResponse;

import java.util.List;

public interface CabinClassService {

    CabinClassResponse createCabinClass(Long userId, CabinClassRequest request);
    CabinClassBulkCreateResponse createCabinClasses(Long userId, List<CabinClassRequest> requests);
    CabinClassResponse getCabinClassById(Long userId, Long id);
    List<CabinClassResponse> getCabinClassesByAircraftId(Long userId, Long aircraftId);
    CabinClassResponse getByAircraftIdAndName(Long userId, Long aircraftId, CabinClassType name);
    CabinClassResponse updateCabinClass(Long userId, Long id, CabinClassRequest request);
    void deleteCabinClass(Long userId, Long id);
}
