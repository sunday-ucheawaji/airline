package com.sunday.services.service;

import java.util.UUID;
import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.payload.request.AirlineRequest;
import com.sunday.common_lib.payload.response.AirlineDropdownItem;
import com.sunday.common_lib.payload.response.AirlineResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface AirlineService {

    // ----- CRUD -----
    List<AirlineResponse> getMyAirlines(UUID userId);
    /** Permission names the caller's active membership in this specific airline grants — resolved per (userId, airlineId), never "the user's role" in the abstract. */
    List<String> getMyPermissions(UUID airlineId, UUID userId);
    /**
     * Batch variant: one call instead of one per airline. Throws OperationNotPermittedException
     * naming every airline the caller lacks the permission on (missing membership counts as lacking it).
     */
    void requirePermission(UUID userId, List<UUID> airlineIds, String permission);
    AirlineResponse getAirlineById(UUID id, UUID userId);

    /** No permission check — service-to-service only, for display-enrichment callers with no real caller identity (e.g. flight-ops-service building a FlightResponse). */
    AirlineResponse getAirlineByIdInternal(UUID id);

    /** Batch variant of {@link #getAirlineByIdInternal}: one round trip instead of one per airline. */
    Map<UUID, AirlineResponse> getAirlinesByIdsInternal(List<UUID> ids);

    Page<AirlineResponse> getAllAirlines(Pageable pageable);
    AirlineResponse updateAirline(UUID airlineId, AirlineRequest request, UUID userId);
    void deleteAirline(UUID id, UUID userId);

    AirlineResponse changeStatusByAdmin(UUID airlineId, AirlineStatus status);

    // ----- Dropdown -----
    List<AirlineDropdownItem> getAirlinesForDropdown();
}
