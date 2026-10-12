package com.sunday.services.controller;

import com.sunday.common_lib.exception.BadRequestException;
import com.sunday.common_lib.payload.request.AirportRequest;
import com.sunday.common_lib.payload.response.AirportBulkCreateResponse;
import com.sunday.common_lib.payload.response.ApiResponse;
import com.sunday.common_lib.payload.response.AirportResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.service.AirportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/airports")
@RequiredArgsConstructor
public class AirportController {

    private final AirportService airportService;

    private static final Set<String> SORTABLE_FIELDS = Set.of("iataCode", "name", "timeZoneId");

    @PostMapping
    public ResponseEntity<AirportResponse> createAirport(@Valid @RequestBody AirportRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(airportService.createAirport(request));
    }

    @PostMapping("/bulk")
    public ResponseEntity<AirportBulkCreateResponse> createBulkAirports(
            @Valid @RequestBody List<AirportRequest> requests) {
        return ResponseEntity.status(HttpStatus.CREATED).body(airportService.createBulkAirports(requests));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AirportResponse> getAirportById(@PathVariable UUID id) {
        return ResponseEntity.ok(airportService.getAirportById(id));
    }


    /** Batch variant of {@code GET /{id}}: one round trip instead of one per airport. */
    @PostMapping("/batch")
    public ResponseEntity<Map<UUID, AirportResponse>> getAirportsByIds(@RequestBody List<UUID> ids) {
        return ResponseEntity.ok(airportService.getAirportsByIds(ids));
    }

    @GetMapping
    public ResponseEntity<Page<AirportResponse>> getAllAirports(
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
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(airportService.getAllAirports(pageable));
    }

    @GetMapping("/city/{cityId}")
    public ResponseEntity<List<AirportResponse>> getAirportsByCityId(@PathVariable UUID cityId) {
        return ResponseEntity.ok(airportService.getAirportsByCityId(cityId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AirportResponse> updateAirport(
            @PathVariable UUID id,
            @Valid @RequestBody AirportRequest request) {
        return ResponseEntity.ok(airportService.updateAirport(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteAirport(@PathVariable UUID id) {
        airportService.deleteAirport(id);
        return ResponseEntity.ok(new ApiResponse(ErrorMessageUtil.AIRPORT_DELETED_SUCCESSFULLY));
    }
}
