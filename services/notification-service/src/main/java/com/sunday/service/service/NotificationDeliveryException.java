package com.sunday.service.service;

/** Thrown when a channel send fails, so it propagates out of the @KafkaListener and reaches the
 *  container's configured retry/DLT error handler — never caught-and-swallowed inside the listener. */
public class NotificationDeliveryException extends RuntimeException {
    public NotificationDeliveryException(String channel, Throwable cause) {
        super("Failed to deliver " + channel + " notification: " + cause.getMessage(), cause);
    }
}
