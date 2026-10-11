package com.sunday.services.service;

import com.sunday.common_lib.payload.request.CityRequest;
import com.sunday.common_lib.payload.response.CityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;
import java.util.List;

public interface CityService {

    // ---------- Core CRUD ----------
    CityResponse createCity(CityRequest request);
    List<CityResponse> createBulkCities(List<CityRequest> requests);
    CityResponse getCityById(UUID id);

    CityResponse updateCity(UUID id, CityRequest request);
    void deleteCity(UUID id);
    Page<CityResponse> getAllCities(Pageable pageable);

    // ---------- Search & Query ----------
    Page<CityResponse> searchCities(String keyword, Pageable pageable);
    Page<CityResponse> getCitiesByCountryCode(String countryCode, Pageable pageable);

    // ---------- Validation ----------
    boolean cityExists(String cityCode);
    boolean validateCityCode(String cityCode);
}
