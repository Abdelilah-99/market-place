package com.buy01.payments.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PaymentExceptionHandlerTest {
    @Test
    void exposesSafeConflictReason() {
        var response = new PaymentExceptionHandler().handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient product quantity"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Insufficient product quantity", response.getBody().message());
    }
}
