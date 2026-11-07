package com.cipher.workspace.Exception;

import com.cipher.workspace.DTOs.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        lenient().when(request.getRequestURI()).thenReturn("/api/test");
        lenient().when(request.getMethod()).thenReturn("GET");
    }

    @Test
    void handleValidation() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "email", "Email cannot be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiError> response = exceptionHandler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().message());
        assertNotNull(response.getBody().fieldErrors());
        assertEquals("Email cannot be blank", response.getBody().fieldErrors().get("email"));
    }

    @Test
    void handleConstraintViolation() {
        ConstraintViolationException ex = new ConstraintViolationException("Invalid parameter constraint", Collections.emptySet());
        ResponseEntity<ApiError> response = exceptionHandler.handleConstraintViolation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid parameter constraint", response.getBody().message());
    }

    @Test
    void handleMissingServletRequestParameter() {
        MissingServletRequestParameterException ex = mock(MissingServletRequestParameterException.class);
        when(ex.getMessage()).thenReturn("Required parameter 'startDate' is not present");

        ResponseEntity<ApiError> response = exceptionHandler.handleMissingParam(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Required parameter 'startDate' is not present", response.getBody().message());
    }

    @Test
    void handleBadRequest() {
        ResponseEntity<ApiError> response =
                exceptionHandler.handleBadRequest(new BadRequestException("Invalid input data"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid input data", response.getBody().message());
    }

    @Test
    void handleIllegalArgument() {
        ResponseEntity<ApiError> response =
                exceptionHandler.handleIllegalArgument(new IllegalArgumentException("Month must be between 1 and 12"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Month must be between 1 and 12", response.getBody().message());
    }

    @Test
    void handleIllegalState() {
        ResponseEntity<ApiError> response =
                exceptionHandler.handleIllegalState(new IllegalStateException("Visit already checked out"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Visit already checked out", response.getBody().message());
    }

    @Test
    void handleNotFound() {
        ResponseEntity<ApiError> response =
                exceptionHandler.handleNotFound(new ResourceNotFoundException("Customer not found"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Customer not found", response.getBody().message());
    }

    @Test
    void handleNoResourceFound() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);
        ResponseEntity<ApiError> response = exceptionHandler.handleNoResourceFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Resource not found", response.getBody().message());
    }

    @Test
    void handleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = mock(HttpRequestMethodNotSupportedException.class);
        when(ex.getMethod()).thenReturn("PATCH");

        ResponseEntity<ApiError> response = exceptionHandler.handleMethodNotSupported(ex, request);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals("HTTP method 'PATCH' is not supported for this endpoint", response.getBody().message());
    }

    @Test
    void handleDuplicate() {
        ResponseEntity<ApiError> response =
                exceptionHandler.handleDuplicate(new DuplicateResourceException("Email already exists"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email already exists", response.getBody().message());
    }

    @Test
    void handleUsernameExists() {
        ResponseEntity<ApiError> response =
                exceptionHandler.handleUsernameExists(new UsernameAlreadyExistsException("Username taken"), request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Username taken", response.getBody().message());
    }

    @Test
    void handleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique constraint violation");
        ResponseEntity<ApiError> response = exceptionHandler.handleDataIntegrityViolation(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Database constraint violation: duplicate or conflicting data", response.getBody().message());
    }

    @Test
    void handleBadCredentials() {
        ResponseEntity<ApiError> response =
                exceptionHandler.handleBadCredentials(new BadCredentialsException("Bad"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid username or password", response.getBody().message());
    }

    @Test
    void handleInvalidToken() {
        ResponseEntity<ApiError> response =
                exceptionHandler.handleInvalidToken(new InvalidTokenException("Expired token"), request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Expired token", response.getBody().message());
    }

    @Test
    void handleDisabled() {
        DisabledException ex = new DisabledException("Account deactivated");
        ResponseEntity<ApiError> response = exceptionHandler.handleDisabled(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Account deactivated", response.getBody().message());
    }

    @Test
    void handleAuth() {
        AuthenticationException ex = new AuthenticationException("Bad auth token") {};
        ResponseEntity<ApiError> response = exceptionHandler.handleAuth(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Authentication failed", response.getBody().message());
    }

    @Test
    void handleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden access");
        ResponseEntity<ApiError> response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You do not have permission to access this resource", response.getBody().message());
    }

    @Test
    void handleDataAccessException() {
        InvalidDataAccessResourceUsageException ex = new InvalidDataAccessResourceUsageException("column status not found");
        ResponseEntity<ApiError> response = exceptionHandler.handleDataAccessException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("A database error occurred. Please try again later.", response.getBody().message());
    }

    @Test
    void handleGeneric() {
        ResponseEntity<ApiError> response =
                exceptionHandler.handleGeneric(new RuntimeException("Crash"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An internal server error occurred. Please try again later.", response.getBody().message());
    }
}
