package com.sunday.common_lib.exception;

/** A dependency this request needs is unreachable, so the request fails closed. Mapped to 503. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
