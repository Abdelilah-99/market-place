package com.buy01.users.Exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.AccessDeniedException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import com.buy01.users.Utils.ApiResponseUtils;

class GlobalExceptionsTest {

    private GlobalExceptions globalExceptions;

    @BeforeEach
    void setUp() {
        globalExceptions = new GlobalExceptions();
    }

    @Test
    void mapsUsernameNotFoundToUnauthorized() {
        ResponseEntity<ApiResponseUtils<String>> response = globalExceptions
                .handleUsernameNotFoundException(new UsernameNotFoundException("missing user"));

        assertError(response, HttpStatus.UNAUTHORIZED, "missing user");
    }

    @Test
    void mapsBadCredentialsToUnauthorized() {
        ResponseEntity<ApiResponseUtils<String>> response = globalExceptions
                .handleBadCredentialsException(new BadCredentialsException("bad"));

        assertError(response, HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @Test
    void mapsAccessDeniedToForbidden() {
        ResponseEntity<ApiResponseUtils<String>> response = globalExceptions
                .handleAccessDeniedException(new AccessDeniedException("secret"));

        assertError(response, HttpStatus.FORBIDDEN, "Not allowed to access");
    }

    @Test
    void mapsMethodNotAllowedWithMethodName() {
        ResponseEntity<ApiResponseUtils<String>> response = globalExceptions
                .handleMethodNotAllowed(new HttpRequestMethodNotSupportedException("PATCH", List.of("GET")));

        assertError(response, HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not allowed: PATCH");
    }

    @Test
    void mapsMessageConversionToBadRequest() {
        ResponseEntity<ApiResponseUtils<String>> response = globalExceptions
                .handleHttpMessageConversionException(new HttpMessageConversionException("bad json"));

        assertError(response, HttpStatus.BAD_REQUEST, "bad json");
    }

    @Test
    void mapsUserExistsToConflict() {
        ResponseEntity<ApiResponseUtils<String>> response = globalExceptions
                .handleUserExistException(new UserExistException("email exists"));

        assertError(response, HttpStatus.CONFLICT, "email exists");
    }

    @Test
    void mapsTypeMismatchWithParameterName() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "page", null, new IllegalArgumentException("bad page"));

        ResponseEntity<ApiResponseUtils<String>> response = globalExceptions
                .handleMethodArgumentTypeMismatchException(ex);

        assertError(response, HttpStatus.BAD_REQUEST, "Invalid parameter: page");
    }

    @Test
    void mapsIllegalArgumentToBadRequest() {
        ResponseEntity<ApiResponseUtils<String>> response = globalExceptions
                .handleIllegalArgumentException(new IllegalArgumentException("invalid role"));

        assertError(response, HttpStatus.BAD_REQUEST, "invalid role");
    }

    @Test
    void mapsResponseStatusExceptionToItsStatus() {
        ResponseEntity<ApiResponseUtils<String>> response = globalExceptions
                .handleResponseStatusException(new ResponseStatusException(HttpStatus.BAD_REQUEST, "self delete"));

        assertError(response, HttpStatus.BAD_REQUEST, "self delete");
    }

    @Test
    void mapsUnexpectedExceptionToInternalServerError() {
        ResponseEntity<ApiResponseUtils<String>> response = globalExceptions
                .handleAllExceptions(new RuntimeException("boom"));

        assertError(response, HttpStatus.INTERNAL_SERVER_ERROR, "boom");
    }

    private static void assertError(
            ResponseEntity<ApiResponseUtils<String>> response,
            HttpStatus status,
            String message) {
        assertEquals(status, response.getStatusCode());
        assertEquals(status.value(), response.getBody().statusCode());
        assertEquals(message, response.getBody().message());
        assertFalse(response.getBody().success());
    }
}
