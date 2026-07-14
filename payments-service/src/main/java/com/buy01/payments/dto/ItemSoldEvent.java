package com.buy01.payments.dto;

import java.time.Instant;

public record ItemSoldEvent(
        String eventId,
        String reservationId,
        String stripeSessionId,
        String productId,
        long quantity,
        String buyerId,
        Instant soldAt) {
}
