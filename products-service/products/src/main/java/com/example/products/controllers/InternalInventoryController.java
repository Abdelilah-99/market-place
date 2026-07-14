package com.example.products.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.products.dto.StockReservationRequest;
import com.example.products.models.Product;
import com.example.products.services.ProductService;
import com.example.shared.common.utils.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/internal/inventory")
public class InternalInventoryController {
    private final ProductService productService;

    public InternalInventoryController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/reservations")
    public ResponseEntity<ApiResponse<Product>> reserve(@RequestBody @Valid StockReservationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(productService.reserveStock(
                request.reservationId(), request.productId(), request.quantity())));
    }

    @PostMapping("/reservations/confirm")
    public ResponseEntity<Void> confirm(@RequestBody @Valid StockReservationRequest request) {
        return productService.confirmReservation(request.reservationId(), request.productId(), request.quantity())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/reservations/release")
    public ResponseEntity<Void> release(@RequestBody @Valid StockReservationRequest request) {
        return productService.releaseReservation(request.reservationId(), request.productId(), request.quantity())
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
