package com.sunday.common_lib.exception;

import com.sunday.common_lib.util.ErrorMessageUtil;
import org.springframework.http.HttpStatus;

/** Account and session failures, each with the HTTP status and stable code a client can branch on. */
public enum AuthError {

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, ErrorMessageUtil.INVALID_CREDENTIALS),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, ErrorMessageUtil.EMAIL_NOT_VERIFIED),
    ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, ErrorMessageUtil.ACCOUNT_LOCKED),
    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN, ErrorMessageUtil.ACCOUNT_NOT_ACTIVE),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, ErrorMessageUtil.INVALID_REFRESH_TOKEN),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, ErrorMessageUtil.REFRESH_TOKEN_EXPIRED),
    REFRESH_TOKEN_REUSE(HttpStatus.UNAUTHORIZED, ErrorMessageUtil.REFRESH_TOKEN_REUSE_DETECTED),
    INVALID_VERIFICATION_TOKEN(HttpStatus.BAD_REQUEST, ErrorMessageUtil.INVALID_VERIFICATION_TOKEN),
    VERIFICATION_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, ErrorMessageUtil.VERIFICATION_TOKEN_EXPIRED),
    VERIFICATION_TOKEN_USED(HttpStatus.BAD_REQUEST, ErrorMessageUtil.VERIFICATION_TOKEN_ALREADY_USED),
    INVALID_RESET_TOKEN(HttpStatus.BAD_REQUEST, ErrorMessageUtil.INVALID_PASSWORD_RESET_TOKEN),
    CURRENT_PASSWORD_INCORRECT(HttpStatus.BAD_REQUEST, ErrorMessageUtil.CURRENT_PASSWORD_INCORRECT),
    PASSWORD_UNCHANGED(HttpStatus.BAD_REQUEST, ErrorMessageUtil.PASSWORD_UNCHANGED),
    PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, ErrorMessageUtil.PASSWORD_TOO_LONG);

    private final HttpStatus status;
    private final String message;

    AuthError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
