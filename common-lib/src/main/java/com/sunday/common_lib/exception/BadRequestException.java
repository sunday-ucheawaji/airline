package com.sunday.common_lib.exception;

/** The request is syntactically fine but semantically invalid (bad format, out-of-range value). Mapped to 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
