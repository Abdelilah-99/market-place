package com.buy01.payments.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCheckoutSessionRequest(
        @NotBlank String productId,
        @NotBlank String productName,
        @NotNull @DecimalMin("0.50") BigDecimal amount,
        String currency,
        String imageUrl) {
}
