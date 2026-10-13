package com.sunday.services.Integration.impl;

import java.util.UUID;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.client.AirlineClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AirlineIntegrationServiceImpl implements AirlineIntegrationService {

    private final AirlineClient airlineClient;

    @Override
    public void requireMembership(UUID userId, UUID airlineId) {
        List<AirlineResponse> airlines;
        try {
            airlines = airlineClient.getMyAirlines(userId);
        } catch (FeignException e) {
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.AIRLINE_SERVICE_ERROR, e.getMessage()), e);
        }
        // Fallback returns null when airline-core-service is unreachable
        if (airlines == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
        }
        // /mine only lists airlines where the caller holds an ACTIVE membership
        boolean isMember = airlines.stream().anyMatch(airline -> airlineId.equals(airline.getId()));
        if (!isMember) {
            throw new OperationNotPermittedException(
                    "You are not an active member of airline " + airlineId);
        }
    }

    @Override
    public void requirePermission(UUID userId, UUID airlineId, String permission) {
        List<String> permissions;
        try {
            permissions = airlineClient.getMyPermissions(airlineId, userId);
        } catch (FeignException.Forbidden e) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRLINE, permission, airlineId));
        } catch (FeignException e) {
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.AIRLINE_SERVICE_ERROR, e.getMessage()), e);
        }
        // Fallback returns null when airline-core-service is unreachable
        if (permissions == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
        }
        if (!permissions.contains(permission)) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRLINE, permission, airlineId));
        }
    }

    @Override
    public void requirePermission(UUID userId, Collection<UUID> airlineIds, String permission) {
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
    public AircraftResponse getAircraftById(UUID aircraftId) {
        AircraftResponse response;
        try {
            response = airlineClient.getAircraftById(aircraftId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException(String.format(ErrorMessageUtil.AIRCRAFT_NOT_FOUND_BY_ID, aircraftId));
        } catch (FeignException.Forbidden e) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRCRAFT, aircraftId));
        } catch (FeignException e) {
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.AIRCRAFT_SERVICE_ERROR, e.getMessage()), e);
        }
        if (response == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.AIRLINE_SERVICE_UNAVAILABLE);
        }
        return response;
    }
}
