package com.sunday.common_lib.exception;


import com.sunday.common_lib.payload.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Centralised exception handling for every service that depends on common-lib.
 *
 * Registered automatically via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * (see {@link GlobalExceptionHandlerAutoConfiguration}) because this package sits
 * outside each service's component-scan root.
 *
 * Handlers for the common-lib domain exceptions return the shared
 * {@link ErrorResponse} body: {@code { timestamp, status, error, message, path }}.
 * Standard Spring MVC exceptions (wrong method, unreadable body, unsupported media
 * type, …) keep their framework-provided status via {@link ResponseEntityExceptionHandler}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /* ------------------------------------------------------------------ */
    /*  Custom exceptions from common-lib                                  */
    /* ------------------------------------------------------------------ */

    /** Entity lookups that returned nothing. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** City not found. */
    @ExceptionHandler(CityException.class)
    public ResponseEntity<ErrorResponse> handleCity(
            CityException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /** Airport conflicts (duplicate IATA code) and lookups. */
    @ExceptionHandler(AirportException.class)
    public ResponseEntity<ErrorResponse> handleAirport(
            AirportException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Action the caller is not allowed to perform / duplicate resource. */
    @ExceptionHandler(OperationNotPermittedException.class)
    public ResponseEntity<ErrorResponse> handleOperationNotPermitted(
            OperationNotPermittedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    /** Auth / user domain failures (bad credentials, duplicate email, etc.). */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUser(
            UserException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /** Payment initiation / verification / gateway failures. */
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePayment(
            PaymentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /* ------------------------------------------------------------------ */
    /*  Catch-all — never leak stack traces to the client                  */
    /* ------------------------------------------------------------------ */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", request);
    }

    /* ------------------------------------------------------------------ */
    /*  Bean validation (@Valid request bodies) — override framework hook  */
    /* ------------------------------------------------------------------ */

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Validation failed";
        }

        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                message,
                pathOf(request)
        );
        return ResponseEntity.badRequest().body(body);
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String message, HttpServletRequest request) {

        ErrorResponse response = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}
