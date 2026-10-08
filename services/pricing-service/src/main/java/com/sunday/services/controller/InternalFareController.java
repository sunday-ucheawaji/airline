package com.sunday.services.controller;

import com.sunday.common_lib.enums.CabinClassType;
import com.sunday.common_lib.payload.request.FareSearchRequest;
import com.sunday.common_lib.payload.response.FareQuoteResponse;
import com.sunday.common_lib.payload.response.FareResponse;
import com.sunday.common_lib.payload.response.FareSearchResult;
import com.sunday.services.service.FareQuoteService;
import com.sunday.services.service.FareSearchService;
import com.sunday.services.service.FareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service-to-service only: {@code /internal/**} is not routed by the api-gateway.
 * No caller identity is forwarded here, so these endpoints are not permission-checked.
 */
@RestController
@RequestMapping("/internal/fares")
@RequiredArgsConstructor
public class InternalFareController {

    private final FareService fareService;
    private final FareQuoteService fareQuoteService;
    private final FareSearchService fareSearchService;

    @GetMapping("/{id}")
    public FareResponse getFareById(@PathVariable Long id) {
        return fareService.getFareByIdInternal(id);
    }

    @PostMapping("/batch")
    public Map<Long, FareResponse> getFaresByIds(@RequestBody List<Long> ids) {
        return fareService.getFaresByIds(ids);
    }

    @GetMapping("/{id}/quote")
    public FareQuoteResponse quote(
            @PathVariable Long id,
            @RequestParam Long flightId,
            @RequestParam CabinClassType cabinClass,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate) {
        return fareQuoteService.quote(id, flightId, cabinClass, departureDate);
    }

    @PostMapping("/search")
    public List<FareSearchResult> search(@Valid @RequestBody FareSearchRequest request) {
        return fareSearchService.search(request);
    }
}
