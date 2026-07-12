package com.buy01.payments.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.buy01.payments.dto.CheckoutSessionResponse;
import com.buy01.payments.dto.CreateCheckoutSessionRequest;
import com.buy01.payments.service.StripeCheckoutService;
import com.stripe.exception.StripeException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final StripeCheckoutService stripeCheckoutService;

    public PaymentController(StripeCheckoutService stripeCheckoutService) {
        this.stripeCheckoutService = stripeCheckoutService;
    }

    @PostMapping("/checkout-sessions")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @RequestBody @Valid CreateCheckoutSessionRequest request,
            @RequestHeader(name = "X-User-Id", required = false) String userId) {
        try {
            return ResponseEntity.ok(stripeCheckoutService.createCheckoutSession(request, userId));
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), e);
        } catch (StripeException e) {
            log.error("Stripe checkout failed: type={}, code={}, requestId={}",
                    e.getClass().getSimpleName(), e.getCode(), e.getRequestId(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stripe checkout session creation failed", e);
        }
    }
}
