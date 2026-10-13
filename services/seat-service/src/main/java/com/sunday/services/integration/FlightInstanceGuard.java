package com.sunday.services.integration;

import com.sunday.common_lib.exception.ConflictException;
import com.sunday.common_lib.exception.ServiceUnavailableException;
import com.sunday.common_lib.payload.response.FlightInstanceResponse;
import com.sunday.common_lib.util.ErrorMessageUtil;
import com.sunday.services.client.FlightOpsClient;
import com.sunday.services.model.CabinClass;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

/** Manual inventory may only be attached to a flight instance operated by the aircraft the cabin belongs to. */
@Component
@RequiredArgsConstructor
public class FlightInstanceGuard {

    private final FlightOpsClient flightOpsClient;

    /** Returns the flight instance, so the caller takes its flight id from flight-ops rather than from the request. */
    public FlightInstanceResponse requireOperatedBy(UUID flightInstanceId, CabinClass cabinClass) {
        FlightInstanceResponse instance = flightOpsClient.getFlightInstance(flightInstanceId);
        if (instance == null) {
            throw new ServiceUnavailableException(ErrorMessageUtil.FLIGHT_OPS_SERVICE_UNAVAILABLE);
        }
        if (!Objects.equals(instance.getAircraftId(), cabinClass.getAircraftId())) {
            throw new ConflictException(String.format(ErrorMessageUtil.FLIGHT_INSTANCE_AIRCRAFT_MISMATCH,
                    flightInstanceId, instance.getAircraftId(), cabinClass.getId()));
        }
        return instance;
    }
}
