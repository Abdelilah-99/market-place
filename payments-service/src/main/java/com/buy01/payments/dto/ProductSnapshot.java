package com.buy01.payments.dto;

import java.math.BigDecimal;

public record ProductSnapshot(
        String id,
        String name,
        BigDecimal price,
        long quantity,
        String imageUrl) {
}
