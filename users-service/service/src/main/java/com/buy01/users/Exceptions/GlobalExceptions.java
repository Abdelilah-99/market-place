package com.buy01.users.Exceptions;

import java.nio.file.AccessDeniedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.buy01.users.Utils.ApiResponseUtils;

@ControllerAdvice
public class GlobalExceptions {
        @ExceptionHandler(UsernameNotFoundException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleUsernameNotFoundException(UsernameNotFoundException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                ex.getMessage(),
                                HttpStatus.UNAUTHORIZED);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleBadCredentialsException(BadCredentialsException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                "Invalid credentials",
                                HttpStatus.UNAUTHORIZED);
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleAccessDeniedException(AccessDeniedException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                "Not allowed to access",
                                HttpStatus.FORBIDDEN);
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        }

        @ExceptionHandler(NoHandlerFoundException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                "Path not found",
                                HttpStatus.NOT_FOUND);
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleMethodNotAllowed(
                        HttpRequestMethodNotSupportedException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                "HTTP method not allowed: " + ex.getMethod(),
                                HttpStatus.METHOD_NOT_ALLOWED);
                return new ResponseEntity<>(response, HttpStatus.METHOD_NOT_ALLOWED);
        }

        @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleHttpMediaTypeNotSupported(
                        HttpMediaTypeNotSupportedException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                "Media type not supported: " + ex.getContentType(),
                                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                return new ResponseEntity<>(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        @ExceptionHandler(HttpMessageConversionException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleHttpMessageConversionException(
                        HttpMessageConversionException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleMissingServletRequestParameterException(
                        MissingServletRequestParameterException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleMethodArgumentNotValidException(
                        MethodArgumentNotValidException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                ex.getBindingResult().getAllErrors().get(0).getDefaultMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(UserExistException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleUserExistException(
                        UserExistException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                ex.getMessage(),
                                HttpStatus.CONFLICT);
                return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MissingPathVariableException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleMissingPathVariableException(
                        MissingPathVariableException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleMethodArgumentTypeMismatchException(
                        MethodArgumentTypeMismatchException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                "Invalid parameter: " + ex.getName(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleIllegalArgumentException(IllegalArgumentException ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST);
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ApiResponseUtils<String>> handleResponseStatusException(ResponseStatusException ex) {
                HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                ex.getReason(),
                                status);
                return new ResponseEntity<>(response, status);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponseUtils<String>> handleAllExceptions(Exception ex) {
                ApiResponseUtils<String> response = ApiResponseUtils.error(
                                ex.getMessage(),
                                HttpStatus.INTERNAL_SERVER_ERROR);
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
}
