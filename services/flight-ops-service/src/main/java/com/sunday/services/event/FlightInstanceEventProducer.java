package com.sunday.services.event;

import com.sunday.common_lib.event.FlightInstanceAircraftChangedEvent;
import com.sunday.common_lib.event.FlightInstanceCreatedEvent;
import com.sunday.common_lib.event.FlightInstancesBulkCreatedEvent;
import com.sunday.common_lib.event.FlightInstancesDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Enqueues onto the outbox (see {@link OutboxEventPublisher}) instead of calling Kafka directly —
 * the actual send happens asynchronously via {@link OutboxEventSweeper}, atomically with whatever
 * business transaction the caller is in.
 */
@Service
@RequiredArgsConstructor
public class FlightInstanceEventProducer {

    private final OutboxEventPublisher outboxEventPublisher;

    public void sendFlightInstanceCreated(FlightInstanceCreatedEvent event) {
        outboxEventPublisher.enqueue("flight-instance-created", event);
    }

    /** One event for the whole batch instead of one FlightInstanceCreatedEvent per instance — see FlightInstancesBulkCreatedEvent. */
    public void sendFlightInstancesBulkCreated(FlightInstancesBulkCreatedEvent event) {
        outboxEventPublisher.enqueue("flight-instances-bulk-created", event);
    }

    public void sendFlightInstancesDeleted(FlightInstancesDeletedEvent event) {
        outboxEventPublisher.enqueue(FlightInstancesDeletedEvent.TOPIC, event);
    }

    public void sendFlightInstanceAircraftChanged(FlightInstanceAircraftChangedEvent event) {
        outboxEventPublisher.enqueue("flight-instance-aircraft-changed", event);
    }
}
