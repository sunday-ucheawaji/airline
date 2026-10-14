package com.sunday.services.integration.impl;

import java.util.UUID;
import com.sunday.common_lib.enums.AirlineStatus;
import com.sunday.common_lib.exception.OperationNotPermittedException;
import com.sunday.common_lib.exception.ResourceNotFoundException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.AirlineResponse;
import com.sunday.common_lib.payload.response.FlightResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.client.AirlineClient;
import com.sunday.services.client.FlightClient;
import com.sunday.services.integration.PricingIntegrationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingIntegrationServiceImpl implements PricingIntegrationService {

    private final AirlineClient airlineClient;
    private final FlightClient flightClient;

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
        if (permissions == null || !permissions.contains(permission)) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRLINE, permission, airlineId));
        }
    }

    @Override
    public void requirePermission(UUID userId, Collection<UUID> airlineIds, String permission) {
        if (airlineIds.isEmpty()) {
            return;
        }
        List<UUID> ids = new ArrayList<>(airlineIds);
        try {
            airlineClient.requirePermission(ids, permission, userId);
        } catch (FeignException.Forbidden e) {
            throw new OperationNotPermittedException(
                    String.format(ErrorMessageUtil.NO_PERMISSION_FOR_AIRLINES, permission, ids));
        } catch (FeignException e) {
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.AIRLINE_SERVICE_ERROR, e.getMessage()), e);
        }
    }

    @Override
    public Map<UUID, UUID> findAirlineIdsByFlightIds(Collection<UUID> flightIds) {
        if (flightIds.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = new ArrayList<>(flightIds);
        Map<UUID, FlightResponse> flights;
        try {
            flights = flightClient.getFlightsByIds(ids);
        } catch (FeignException e) {
            throw new ServiceUnavailableException(String.format(ErrorMessageUtil.FLIGHT_SERVICE_ERROR, e.getMessage()), e);
        }
        if (flights == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE);
        }
        Map<UUID, UUID> airlineByFlight = new HashMap<>();
        for (UUID flightId : ids) {
            FlightResponse flight = flights.get(flightId);
            if (flight == null) {
                continue;
            }
            if (flight.getAirline() == null || flight.getAirline().getId() == null) {
                throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_SERVICE_UNAVAILABLE);
            }
            airlineByFlight.put(flightId, flight.getAirline().getId());
        }
        return airlineByFlight;
    }

    @Override
    public Map<UUID, UUID> resolveAirlineIdsByFlightIds(Collection<UUID> flightIds) {
        Map<UUID, UUID> airlineByFlight = findAirlineIdsByFlightIds(flightIds);
        for (UUID flightId : flightIds) {
            if (!airlineByFlight.containsKey(flightId)) {
                throw new ResourceNotFoundException(String.format(ErrorMessageUtil.FLIGHT_NOT_FOUND_BY_ID, flightId));
            }
        }
        return airlineByFlight;
    }

    @Override
    public boolean isAirlineKnownInactive(UUID airlineId) {
        AirlineResponse airline;
        try {
            airline = airlineClient.getAirlineByIdInternal(airlineId);
        } catch (FeignException.NotFound e) {
            return true;
        } catch (FeignException e) {
            log.warn("Airline {} status unknown, fares are shown: {}", airlineId, e.getMessage());
            return false;
        }
        return airline != null && airline.getStatus() != null && airline.getStatus() != AirlineStatus.ACTIVE;
    }
}
