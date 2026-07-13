package com.buy01.payments.service;

import java.time.Instant;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.buy01.payments.dto.ItemSoldEvent;

@Service
public class SaleEventPublisher {
    public static final String TOPIC = "item-sold-events";
    private final KafkaTemplate<String, ItemSoldEvent> kafkaTemplate;

    public SaleEventPublisher(KafkaTemplate<String, ItemSoldEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String sessionId, String productId, long quantity, String buyerId) {
        // Stripe retries webhooks. The stable session id is the idempotency key consumed
        // by the products service, so a retry cannot decrement stock twice.
        ItemSoldEvent event = new ItemSoldEvent(sessionId, sessionId,
                productId, quantity, buyerId, Instant.now());
        kafkaTemplate.send(TOPIC, productId, event);
    }
}
