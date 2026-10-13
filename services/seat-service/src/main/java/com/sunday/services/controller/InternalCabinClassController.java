package com.sunday.services.controller;

import com.sunday.common_lib.payload.response.CabinClassResponse;
import com.sunday.services.service.CabinClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Unauthenticated, not routed by the gateway: flight-ops' capacity check before an aircraft reassignment. */
@RestController
@RequestMapping("/internal/cabin-classes")
@RequiredArgsConstructor
public class InternalCabinClassController {

    private final CabinClassService cabinClassService;

    @GetMapping("/aircraft/{aircraftId}")
    public List<CabinClassResponse> getCabinClassesByAircraftId(@PathVariable UUID aircraftId) {
        return cabinClassService.getCabinClassesByAircraftIdInternal(aircraftId);
    }
}
