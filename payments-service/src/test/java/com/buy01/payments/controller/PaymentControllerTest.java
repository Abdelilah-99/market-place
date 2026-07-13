package com.buy01.payments.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.buy01.payments.dto.CheckoutSessionResponse;
import com.buy01.payments.dto.CreateCheckoutSessionRequest;
import com.buy01.payments.service.StripeCheckoutService;
import com.stripe.exception.ApiException;
import com.stripe.exception.StripeException;

class PaymentControllerTest {

    private final CreateCheckoutSessionRequest request = new CreateCheckoutSessionRequest(
            "9f834ed8-c8a8-4e68-8647-a3e12bcb61f2", 1);

    @Test
    void returnsCreatedCheckoutSession() throws Exception {
        CheckoutSessionResponse checkout = new CheckoutSessionResponse("cs_1", "https://checkout.test");
        StripeCheckoutService service = serviceReturning(checkout);

        ResponseEntity<CheckoutSessionResponse> response =
                new PaymentController(service).createCheckoutSession(request, "user-1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(checkout, response.getBody());
    }

    @Test
    void mapsMissingConfigurationToServiceUnavailable() throws Exception {
        StripeCheckoutService service = serviceThrowing(new IllegalStateException("missing"));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> new PaymentController(service).createCheckoutSession(request, null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    @Test
    void mapsStripeFailureToBadGateway() throws Exception {
        StripeCheckoutService service = serviceThrowing(
                new ApiException("Stripe unavailable", null, null, 500, null));

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> new PaymentController(service).createCheckoutSession(request, null));

        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatusCode());
    }

    private StripeCheckoutService serviceReturning(CheckoutSessionResponse response) {
        return new StripeCheckoutService("", "") {
            @Override
            public CheckoutSessionResponse createCheckoutSession(
                    CreateCheckoutSessionRequest ignoredRequest, String ignoredUserId) {
                return response;
            }
        };
    }

    private StripeCheckoutService serviceThrowing(Exception exception) {
        return new StripeCheckoutService("", "") {
            @Override
            public CheckoutSessionResponse createCheckoutSession(
                    CreateCheckoutSessionRequest ignoredRequest, String ignoredUserId) throws StripeException {
                if (exception instanceof StripeException stripeException) {
                    throw stripeException;
                }
                throw (RuntimeException) exception;
            }
        };
    }
}
