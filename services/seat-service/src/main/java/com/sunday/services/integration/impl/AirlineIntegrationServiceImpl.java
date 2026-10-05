package com.sunday.services.integration.impl;

import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.client.AirlineClient;
import com.sunday.services.integration.AirlineIntegrationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
        } catch (FeignException.Forbidden e) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRLINE, permission, airlineId));
        } catch (FeignException e) {
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.AIRLINE_SERVICE_ERROR, e.getMessage()), e);
        }
        if (permissions == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
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
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.AIRLINE_SERVICE_ERROR, e.getMessage()), e);
        }
    }

    @Override
    public Long resolveAirlineIdForAircraft(Long aircraftId) {
        return getAircraftByIdInternal(aircraftId).getAirlineId();
    }

    @Override
    public Map<Long, Long> resolveAirlineIdsForAircrafts(Collection<Long> aircraftIds) {
        if (aircraftIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, AircraftResponse> aircrafts;
        try {
            aircrafts = airlineClient.getAircraftsByIdsInternal(new ArrayList<>(aircraftIds));
        } catch (FeignException e) {
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.AIRCRAFT_SERVICE_ERROR, e.getMessage()), e);
        }
        if (aircrafts == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
        }
        Map<Long, Long> airlineIdByAircraft = new HashMap<>();
        aircrafts.forEach((aircraftId, aircraft) -> {
            if (aircraft != null && aircraft.getAirlineId() != null) {
                airlineIdByAircraft.put(aircraftId, aircraft.getAirlineId());
            }
        });
        return airlineIdByAircraft;
    }

    @Override
    public AircraftResponse getAircraftByIdInternal(Long aircraftId) {
        AircraftResponse response;
        try {
            response = airlineClient.getAircraftByIdInternal(aircraftId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRCRAFT_NOT_FOUND_BY_ID, aircraftId));
        } catch (FeignException e) {
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.AIRCRAFT_SERVICE_ERROR, e.getMessage()), e);
        }
        if (response == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
        }
        return response;
    }
}
