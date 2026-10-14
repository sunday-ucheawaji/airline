package com.sunday.common_lib.exception;

import org.springframework.http.HttpStatus;

/** A user or account failure. Built from an {@link AuthError} it carries that status and code; otherwise 400. */
public class UserException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public UserException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.code = "BAD_REQUEST";
    }

    public UserException(AuthError error) {
        super(error.message());
        this.status = error.status();
        this.code = error.name();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
