package com.sunday.services.Integration.impl;

import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.payload.response.AircraftResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.services.Integration.AirlineIntegrationService;
import com.sunday.services.client.AirlineClient;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
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
    public void requireMembership(Long userId, Long airlineId) {
        List<AirlineResponse> airlines;
        try {
            airlines = airlineClient.getMyAirlines(userId);
        } catch (FeignException e) {
            throw new RuntimeException("Airline service error: " + e.getMessage(), e);
        }
        // Fallback returns null when airline-core-service is unreachable
        if (airlines == null) {
            throw new RuntimeException("Airline service unavailable");
        }
        // /mine only lists airlines where the caller holds an ACTIVE membership
        boolean isMember = airlines.stream().anyMatch(airline -> airlineId.equals(airline.getId()));
        if (!isMember) {
            throw new OperationNotPermittedException(
                    "You are not an active member of airline " + airlineId);
        }
    }

    @Override
    public void requirePermission(Long userId, Long airlineId, String permission) {
        List<String> permissions;
        try {
            permissions = airlineClient.getMyPermissions(airlineId, userId);
        } catch (FeignException e) {
            throw new RuntimeException("Airline service error: " + e.getMessage(), e);
        }
        // Fallback returns null when airline-core-service is unreachable
        if (permissions == null) {
            throw new RuntimeException("Airline service unavailable");
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
            throw new RuntimeException("Airline service error: " + e.getMessage(), e);
        }
    }

    @Override
    public AircraftResponse getAircraftById(Long aircraftId) {
        try {
            return airlineClient.getAircraftById(aircraftId);
        } catch (FeignException.NotFound e) {
            throw new EntityNotFoundException("No aircraft found for id: " + aircraftId);
        } catch (FeignException e) {
            throw new RuntimeException("Aircraft service error: " + e.getMessage(), e);
        }
    }
}
