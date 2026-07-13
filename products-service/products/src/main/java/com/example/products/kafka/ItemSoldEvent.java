package com.example.products.kafka;

import java.time.Instant;

public record ItemSoldEvent(
        String eventId,
        String stripeSessionId,
        String productId,
        long quantity,
        String buyerId,
        Instant soldAt) {
}
