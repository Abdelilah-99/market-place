package com.example.products.exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;


import com.example.shared.common.utils.ApiResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ApiResponse<String>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                ex.getMessage(),
                                HttpStatus.UNAUTHORIZED);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponse<String>> handleAccessDeniedException(AccessDeniedException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                "Not allowed to access",
                                HttpStatus.FORBIDDEN);
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(NoHandlerFoundException.class)
        public ResponseEntity<ApiResponse<String>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                "Path not found",
                                HttpStatus.NOT_FOUND);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponse<String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                "HTTP method not allowed: " + ex.getMethod(),
                                HttpStatus.METHOD_NOT_ALLOWED);
                return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ApiResponse<String>> handleHttpMediaTypeNotSupported(
                        HttpMediaTypeNotSupportedException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                "Media type not supported: " + ex.getContentType(),
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        @ExceptionHandler(HttpMessageConversionException.class)
        public ResponseEntity<ApiResponse<String>> handleHttpMessageConversionException(
                        HttpMessageConversionException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ApiResponse<String>> handleConstraintViolationException(ConstraintViolationException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiResponse<String>> handleMissingServletRequestParameterException(
                        MissingServletRequestParameterException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponse<String>> handleMethodArgumentNotValidException(
                        MethodArgumentNotValidException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                ex.getBindingResult().getAllErrors().get(0).getDefaultMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<String>> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MissingPathVariableException.class)
        public ResponseEntity<ApiResponse<String>> handleMissingPathVariableException(MissingPathVariableException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiResponse<String>> handleMethodArgumentTypeMismatchException(
                        MethodArgumentTypeMismatchException ex) {
                ApiResponse<String> response = ApiResponse.error(
                                "Invalid parameter: " + ex.getName(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiResponse<String>> handleResponseStatusException(ResponseStatusException ex) {
                HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
                ApiResponse<String> response = ApiResponse.error(
                                ex.getReason(),
                                status);
                return new ResponseEntity<>(response, status);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<String>> handleAllExceptions(Exception ex) {
                ApiResponse<String> response = ApiResponse.error(
                                ex.getMessage(),
                                HttpStatus.INTERNAL_SERVER_ERROR);
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}
