package com.sunday.common_lib.exception;


import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.sunday.common_lib.util.ErrorMessageUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Centralised exception handling for every service that depends on common-lib.
 *
 * Registered automatically via
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * (see {@link GlobalExceptionHandlerAutoConfiguration}) because this package sits
 * outside each service's component-scan root.
 *
 * Every error is an RFC 9457 problem ({@code application/problem+json}): {@code type, title, status, detail,
 * instance}, plus a stable {@code code} and a {@code timestamp}, and for validation failures an {@code errors} list
 * of {@code {field, code, message}}. {@code detail} is always safe to show to the user. Standard Spring MVC
 * exceptions (wrong method, unsupported media type, missing parameter, …) keep their framework status and get the
 * same extra properties in {@link #createResponseEntity}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** One entry of a problem's {@code errors}: the offending field, the constraint it broke, and what to tell the user. */
    public record FieldViolation(String field, String code, String message) {}

    /* ------------------------------------------------------------------ */
    /*  Custom exceptions from common-lib                                  */
    /* ------------------------------------------------------------------ */

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(OperationNotPermittedException.class)
    public ProblemDetail handleOperationNotPermitted(OperationNotPermittedException ex, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
    }

    /** Two requests changed the same {@code @Version}ed row at once; the loser gets 409 instead of a 500. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "CONFLICT", ErrorMessageUtil.CONCURRENT_MODIFICATION, request);
    }

    /** A unique or foreign key rejected the write, e.g. two requests creating the same row at once. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "CONFLICT", ErrorMessageUtil.DATA_INTEGRITY_CONFLICT, request);
    }

    /** A required downstream service is unreachable; the request fails closed. */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ProblemDetail handleServiceUnavailable(ServiceUnavailableException ex, HttpServletRequest request) {
        log.warn("Fail-closed on {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", ex.getMessage(), request);
    }

    /** Account and session failures carry their own status and code (see {@link AuthError}). */
    @ExceptionHandler(UserException.class)
    public ProblemDetail handleUser(UserException ex, HttpServletRequest request) {
        return problem(ex.getStatus(), ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(PaymentException.class)
    public ProblemDetail handlePayment(PaymentException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "PAYMENT_ERROR", ex.getMessage(), request);
    }

    /** Never leak internals: the client gets a generic message, the log gets the stack trace. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ErrorMessageUtil.UNEXPECTED_ERROR, request);
    }

    /* ------------------------------------------------------------------ */
    /*  Validation and unreadable input                                    */
    /* ------------------------------------------------------------------ */

    /** {@code @Valid @RequestBody}: one entry per broken constraint, keyed by the JSON field name. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> violations.add(violationOf(error)));
        ex.getBindingResult().getGlobalErrors().forEach(error -> violations.add(violationOf(error.getObjectName(), error)));
        return validationProblem(violations, headers, request);
    }

    /** Constraints on {@code @RequestParam}/{@code @PathVariable} arguments, or on a validated body via method validation. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors errors) {
                errors.getFieldErrors().forEach(error -> violations.add(violationOf(error)));
            } else {
                String parameter = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(error -> violations.add(violationOf(parameter, error)));
            }
        });
        return validationProblem(violations, headers, request);
    }

    /** A query or path parameter that does not convert, e.g. a malformed UUID or an unknown enum value. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String name = ex.getPropertyName() != null ? ex.getPropertyName() : "parameter";
        String message = String.format(ErrorMessageUtil.INVALID_PARAMETER, name);
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, pathOf(request));
        body.setProperty("errors", List.of(new FieldViolation(name, "TypeMismatch", message)));
        return ResponseEntity.badRequest().headers(headers).body(body);
    }

    /** Malformed JSON, or a value of the wrong type such as an illegal enum constant. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ErrorMessageUtil.MALFORMED_REQUEST;
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            String field = ife.getPath().isEmpty()
                    ? ""
                    : ife.getPath().get(ife.getPath().size() - 1).getFieldName() + ": ";
            String allowed = Arrays.stream(ife.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            message = field + "must be one of [" + allowed + "]";
        }
        return ResponseEntity.badRequest().headers(headers)
                .body(problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", message, pathOf(request)));
    }

    /** Every framework-built response passes through here: give its problem the same {@code code}, {@code timestamp} and {@code instance}. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            if (problem.getProperties() == null || !problem.getProperties().containsKey("code")) {
                HttpStatus status = HttpStatus.resolve(statusCode.value());
                problem.setProperty("code", status != null ? status.name() : "ERROR");
            }
            if (problem.getProperties() == null || !problem.getProperties().containsKey("timestamp")) {
                problem.setProperty("timestamp", Instant.now().toString());
            }
            if (problem.getInstance() == null) {
                problem.setInstance(URI.create(pathOf(request)));
            }
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private ResponseEntity<Object> validationProblem(List<FieldViolation> violations, HttpHeaders headers, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ErrorMessageUtil.VALIDATION_FAILED, pathOf(request));
        body.setProperty("errors", violations);
        return ResponseEntity.badRequest().headers(headers).body(body);
    }

    private static FieldViolation violationOf(FieldError error) {
        return new FieldViolation(error.getField(), error.getCode(), error.getDefaultMessage());
    }

    private static FieldViolation violationOf(String field, MessageSourceResolvable error) {
        String[] codes = error.getCodes();
        String code = error instanceof ObjectError objectError && objectError.getCode() != null
                ? objectError.getCode()
                : codes != null && codes.length > 0 ? codes[codes.length - 1] : "Invalid";
        return new FieldViolation(field, code, error.getDefaultMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail, HttpServletRequest request) {
        return problem(status, code, detail, request.getRequestURI());
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail, String path) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setInstance(URI.create(path));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    private static String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false).replaceFirst("^uri=", "");
    }
}
