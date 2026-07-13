package com.buy01.payments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateCheckoutSessionRequest(
        @NotBlank String productId,
        @Positive long quantity) {
}
