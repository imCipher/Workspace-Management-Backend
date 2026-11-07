package com.cipher.workspace.Exception;

import com.cipher.workspace.DTOs.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400 — Validation errors on @Valid @RequestBody DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("Validation failed on [{} {}]: {}", req.getMethod(), req.getRequestURI(), fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, fieldErrors);
    }

    // 400 — Method-level parameter validation (Spring 6.1+)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidation(HandlerMethodValidationException ex, HttpServletRequest req) {
        log.warn("Handler method validation failed on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Validation failed for request parameters", req, null);
    }

    // 400 — Constraint violations
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        log.warn("Constraint violation on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    // 400 — Missing required query parameters
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        log.warn("Missing parameter on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    // 400 — Explicit domain BadRequestException
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        log.warn("Bad request on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    // 400 — Illegal arguments
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Illegal argument on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    // 400 — Illegal states
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        log.warn("Illegal state on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    // 401 — Bad credentials at sign-in
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        log.warn("Authentication failed (BadCredentials) on [{} {}]", req.getMethod(), req.getRequestURI());
        return build(HttpStatus.UNAUTHORIZED, "Invalid username or password", req, null);
    }

    // 401 — Invalid, expired, or revoked token
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiError> handleInvalidToken(InvalidTokenException ex, HttpServletRequest req) {
        log.warn("Invalid token on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req, null);
    }

    // 401 — General authentication failures
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        log.warn("Authentication exception on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed", req, null);
    }

    // 403 — Account disabled or pending invite
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, HttpServletRequest req) {
        log.warn("Account disabled on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank() && !ex.getMessage().equalsIgnoreCase("User is disabled"))
                ? ex.getMessage()
                : "Account is not active. Please check your invitation email or contact your administrator.";
        return build(HttpStatus.FORBIDDEN, message, req, null);
    }

    // 403 — Access denied / insufficient permissions
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "You do not have permission to access this resource", req, null);
    }

    // 404 — Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("Resource not found on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    // 404 — Static resource or route not found
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest req) {
        log.warn("Route not found [{} {}]", req.getMethod(), req.getRequestURI());
        return build(HttpStatus.NOT_FOUND, "Resource not found", req, null);
    }

    // 405 — Unsupported HTTP method
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        String message = String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod());
        log.warn("Method not supported on [{} {}]: {}", req.getMethod(), req.getRequestURI(), message);
        return build(HttpStatus.METHOD_NOT_ALLOWED, message, req, null);
    }

    // 409 — Duplicate resource
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
        log.warn("Duplicate resource on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    // 409 — Username already exists
    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUsernameExists(UsernameAlreadyExistsException ex, HttpServletRequest req) {
        log.warn("Username already exists on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, null);
    }

    // 409 — Database constraint violation
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.error("Data integrity violation on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.CONFLICT, "Database constraint violation: duplicate or conflicting data", req, null);
    }

    // 500 — Database errors
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> handleDataAccessException(DataAccessException ex, HttpServletRequest req) {
        log.error("Database error occurred while processing [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "A database error occurred. Please try again later.", req, null);
    }

    // 500 — Generic internal server error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("INTERNAL SERVER ERROR on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred. Please try again later.", req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message,
                                           HttpServletRequest req, Map<String, String> fieldErrors) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
