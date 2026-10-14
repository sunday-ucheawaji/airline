package com.sunday.services.service;

import com.sunday.common_lib.event.FlightInstanceAircraftChangedEvent;
import com.sunday.common_lib.event.FlightInstanceCreatedEvent;
import com.sunday.common_lib.event.FlightInstancesDeletedEvent;
import com.sunday.common_lib.event.FlightInstancesBulkCreatedEvent;

public interface CabinInventoryService {

    void materializeForFlightInstance(FlightInstanceCreatedEvent event);

    void materializeForFlightInstances(FlightInstancesBulkCreatedEvent event);

    void reconcileForAircraftChange(FlightInstanceAircraftChangedEvent event);

    void removeForFlightInstances(FlightInstancesDeletedEvent event);
}
