package com.sunday.common_lib.exception;

/** The request conflicts with current state (duplicate unique value, illegal state transition). Mapped to 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
