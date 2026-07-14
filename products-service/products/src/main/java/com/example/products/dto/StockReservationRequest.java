package com.example.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record StockReservationRequest(
        @NotBlank String reservationId,
        @NotBlank String productId,
        @Positive long quantity) {
}
