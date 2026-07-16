package com.buy01.payments.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class PaymentExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<PaymentError> handleResponseStatus(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new PaymentError(
                Instant.now(),
                exception.getStatusCode().value(),
                exception.getReason()));
    }

    record PaymentError(Instant timestamp, int status, String message) { }
}
