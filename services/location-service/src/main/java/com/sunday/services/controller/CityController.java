package com.sunday.services.controller;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.payload.request.CityAirportReassignmentRequest;
import com.sunday.common_lib.payload.request.CityRequest;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.common_lib.payload.response.ApiResponse;
import com.sunday.common_lib.payload.response.CityBulkCreateResponse;
import com.sunday.common_lib.payload.response.CityResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.service.AirportService;
import com.sunday.services.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
@Slf4j
public class CityController {

    private final CityService cityService;
    private final AirportService airportService;

    // Whitelisted so an arbitrary/misspelled field name is a clean 400, not an unmapped
    // PropertyReferenceException surfacing as a raw 500.
    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "name", "cityCode", "countryCode", "countryName", "regionCode", "timeZoneId");

    @PostMapping
    public ResponseEntity<CityResponse> createCity(
            @Valid @RequestBody CityRequest request) {
        CityResponse response = cityService.createCity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    public ResponseEntity<CityBulkCreateResponse> createBulkCities(
            @Valid @RequestBody List<CityRequest> requests) {
        CityBulkCreateResponse response = cityService.createBulkCities(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CityResponse> getCityById(@PathVariable UUID id) {
        return ResponseEntity.ok(cityService.getCityById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CityResponse>> getAllCities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new BadRequestException(String.format(ErrorMessageUtil.SORT_BY_FIELD_INVALID, SORTABLE_FIELDS));
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(ErrorMessageUtil.SORT_DIRECTION_INVALID);
        }
        Sort sort = Sort.by(direction, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(cityService.getAllCities(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CityResponse> updateCity(
            @PathVariable UUID id,
            @Valid @RequestBody CityRequest request) {
        return ResponseEntity.ok(cityService.updateCity(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCity(@PathVariable UUID id) {
        cityService.deleteCity(id);
        return ResponseEntity.ok(new ApiResponse(ErrorMessageUtil.CITY_DELETED_SUCCESSFULLY));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<CityResponse>> searchCities(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(cityService.searchCities(keyword, pageable));
    }

    @GetMapping("/country/{countryCode}")
    public ResponseEntity<Page<CityResponse>> getCitiesByCountryCode(
            @PathVariable String countryCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(cityService.getCitiesByCountryCode(countryCode.toUpperCase(), pageable));
    }

    @GetMapping("/exists/{cityCode}")
    public ResponseEntity<Boolean> checkCityExists(@PathVariable String cityCode) {
        return ResponseEntity.ok(cityService.cityExists(cityCode.toUpperCase()));
    }

    /** Bulk-moves every airport out of {@code id} so the city can then be deleted. */
    @PatchMapping("/{id}/airports/reassign")
    public ResponseEntity<List<AirportResponse>> reassignAirports(
            @PathVariable UUID id,
            @Valid @RequestBody CityAirportReassignmentRequest request) {
        return ResponseEntity.ok(airportService.reassignAirportsToCity(id, request.getTargetCityId()));
    }
}
