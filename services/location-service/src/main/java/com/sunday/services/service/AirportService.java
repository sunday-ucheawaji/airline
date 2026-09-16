package com.sunday.services.service;

import com.sunday.common_lib.payload.request.AirportRequest;
import com.sunday.common_lib.payload.response.AirportResponse;

import java.util.List;

public interface AirportService {

    AirportResponse createAirport(AirportRequest request);
    List<AirportResponse> createBulkAirports(List<AirportRequest> requests);
    AirportResponse getAirportById(Long id);

    List<AirportResponse> getAllAirports();
    AirportResponse updateAirport(Long id, AirportRequest request);
    void deleteAirport(Long id);
    List<AirportResponse> getAirportsByCityId(Long cityId);
}
