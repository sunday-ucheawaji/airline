package com.sunday.services.event;

import com.sunday.common_lib.event.FlightInstanceAircraftChangedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes the aircraft-reassignment event to Kafka only after the database transaction that
 * saved the new aircraftId has committed, so seat-service's reconciliation consumer can never
 * see an event for a reassignment that didn't actually take effect.
 */
@Component
@RequiredArgsConstructor
public class FlightInstanceAircraftChangedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FlightInstanceAircraftChangedEventPublisher.class);

    private final FlightInstanceEventProducer flightInstanceEventProducer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAircraftChanged(FlightInstanceAircraftChangedEvent event) {
        try {
            flightInstanceEventProducer.sendFlightInstanceAircraftChanged(event);
        } catch (RuntimeException e) {
            log.warn("Could not publish aircraft-changed event for flight instance {}", event.getFlightInstanceId(), e);
        }
    }
}
