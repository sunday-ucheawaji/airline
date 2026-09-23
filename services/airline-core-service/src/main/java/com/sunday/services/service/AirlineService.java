package com.sunday.services.service;

import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.payload.request.AirlineRequest;
import com.sunday.common_lib.payload.response.AirlineDropdownItem;
import com.sunday.common_lib.payload.response.AirlineResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AirlineService {

    // ----- CRUD -----
    List<AirlineResponse> getMyAirlines(Long userId);
    /** Permission names the caller's active membership in this specific airline grants — resolved per (userId, airlineId), never "the user's role" in the abstract. */
    List<String> getMyPermissions(Long airlineId, Long userId);
    /**
     * Batch variant: one call instead of one per airline. Throws OperationNotPermittedException
     * naming every airline the caller lacks the permission on (missing membership counts as lacking it).
     */
    void requirePermission(Long userId, List<Long> airlineIds, String permission);
    AirlineResponse getAirlineById(Long id);
    Page<AirlineResponse> getAllAirlines(Pageable pageable);
    AirlineResponse updateAirline(Long airlineId, AirlineRequest request, Long userId);
    void deleteAirline(Long id, Long userId);

    AirlineResponse changeStatusByAdmin(Long airlineId, AirlineStatus status);

    // ----- Dropdown -----
    List<AirlineDropdownItem> getAirlinesForDropdown();
}
