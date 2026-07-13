package com.buy01.payments.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buy01.payments.service.SaleEventPublisher;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

@RestController
@RequestMapping("/api/payments/webhooks")
public class StripeWebhookController {
    private final String webhookSecret;
    private final SaleEventPublisher publisher;

    public StripeWebhookController(@Value("${stripe.webhook-secret:}") String webhookSecret,
            SaleEventPublisher publisher) {
        this.webhookSecret = webhookSecret;
        this.publisher = publisher;
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
                    .ifPresent(this::publish);
        }
        return ResponseEntity.ok().build();
    }

    private void publish(Session session) {
        String productId = session.getMetadata().get("product_id");
        String quantityValue = session.getMetadata().get("quantity");
        if (productId == null || quantityValue == null) {
            return;
        }
        try {
            publisher.publish(session.getId(), productId, Long.parseLong(quantityValue),
                    session.getMetadata().get("buyer_id"));
        } catch (NumberFormatException ignored) {
            // Ignore malformed metadata; only sessions created by this service are valid sales.
        }
    }
}
