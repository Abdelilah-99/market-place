package com.buy01.payments.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buy01.payments.service.OrderService;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

@RestController
@RequestMapping("/api/payments/webhooks")
public class StripeWebhookController {
    private final String webhookSecret;
    private final OrderService orderService;

    public StripeWebhookController(@Value("${stripe.webhook-secret:}") String webhookSecret,
            OrderService orderService) {
        this.webhookSecret = webhookSecret;
        this.orderService = orderService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> receive(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return ResponseEntity.status(503).build();
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
        if ("checkout.session.completed".equals(event.getType())) {
            event.getDataObjectDeserializer().getObject().filter(Session.class::isInstance)
                    .map(Session.class::cast)
                    .ifPresent(session -> orderService.completeStripeSession(session.getId()));
        } else if ("checkout.session.expired".equals(event.getType())) {
            event.getDataObjectDeserializer().getObject().filter(Session.class::isInstance)
                    .map(Session.class::cast)
                    .ifPresent(session -> orderService.expireStripeSession(session.getId()));
        }
        return ResponseEntity.ok().build();
    }
}
