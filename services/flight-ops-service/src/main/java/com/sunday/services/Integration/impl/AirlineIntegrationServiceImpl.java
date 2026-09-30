package com.sunday.services.Integration.impl;

import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.client.AirlineClient;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AirlineIntegrationServiceImpl implements AirlineIntegrationService {

    private final AirlineClient airlineClient;

    @Override
    public void requirePermission(Long userId, Long airlineId, String permission) {
        List<String> permissions;
        try {
            permissions = airlineClient.getMyPermissions(airlineId, userId);
        } catch (FeignException e) {
            throw new RuntimeException(String.format(ErrorMessageUtil.AIRLINE_SERVICE_ERROR, e.getMessage()), e);
        }
        // Fallback returns null when airline-core-service is unreachable
        if (permissions == null) {
            throw new RuntimeException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
        }
        if (!permissions.contains(permission)) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRLINE, permission, airlineId));
        }
    }

    @Override
    public void requirePermission(Long userId, Collection<Long> airlineIds, String permission) {
        if (airlineIds.isEmpty()) {
            return;
        }
        try {
            airlineClient.requirePermission(new ArrayList<>(airlineIds), permission, userId);
        } catch (FeignException.Forbidden e) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRLINES, permission, airlineIds));
        } catch (FeignException e) {
            throw new RuntimeException(String.format(ErrorMessageUtil.AIRLINE_SERVICE_ERROR, e.getMessage()), e);
        }
    }

    @Override
    public AircraftResponse getAircraftById(Long aircraftId, Long userId) {
        try {
            return airlineClient.getAircraftById(aircraftId, userId);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException(String.format(ErrorMessageUtil.AIRCRAFT_NOT_FOUND_BY_ID, aircraftId));
        } catch (FeignException e) {
            throw new RuntimeException(String.format(ErrorMessageUtil.AIRCRAFT_SERVICE_ERROR, e.getMessage()), e);
        }
    }

    @Override
    public AircraftResponse getAircraftByIdInternal(Long aircraftId) {
        try {
            return airlineClient.getAircraftByIdInternal(aircraftId);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException(String.format(ErrorMessageUtil.AIRCRAFT_NOT_FOUND_BY_ID, aircraftId));
        } catch (FeignException e) {
            throw new RuntimeException(String.format(ErrorMessageUtil.AIRCRAFT_SERVICE_ERROR, e.getMessage()), e);
        }
    }

    @Override
    public Map<Long, AircraftResponse> getAircraftsByIdsInternal(Collection<Long> aircraftIds) {
        if (aircraftIds.isEmpty()) {
            return Map.of();
        }
        try {
            return airlineClient.getAircraftsByIdsInternal(new ArrayList<>(aircraftIds));
        } catch (FeignException e) {
            throw new RuntimeException(String.format(ErrorMessageUtil.AIRCRAFT_SERVICE_ERROR, e.getMessage()), e);
        }
    }
}
