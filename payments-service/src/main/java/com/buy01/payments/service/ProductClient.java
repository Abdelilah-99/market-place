package com.buy01.payments.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.buy01.payments.dto.ProductSnapshot;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.micrometer.observation.annotation.Observed;

@Service
public class ProductClient {
    private final RestClient restClient;

    public ProductClient(RestClient.Builder builder,
            @Value("${products.base-url:https://products-service:8000}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Observed(name = "marketplace.payment.product.lookup", contextualName = "lookup-product")
    public ProductSnapshot getAvailableProduct(String productId, long requestedQuantity) {
        try {
            UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product id", e);
        }

        try {
            ProductEnvelope response = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .header("X-User-Role", "GUEST")
                    .retrieve()
                    .body(ProductEnvelope.class);
            ProductSnapshot product = response == null ? null : response.data();
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from products service");
            }
            if (product.price() == null || product.price().signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Product has an invalid price");
            }
            if (product.quantity() < requestedQuantity) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient product quantity");
            }
            return product;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Products service is unavailable", e);
        }
    }

    @Observed(name = "marketplace.payment.stock.reserve", contextualName = "reserve-product-stock")
    public ProductSnapshot reserve(String reservationId, String productId, long quantity) {
        try {
            ProductEnvelope response = restClient.post()
                    .uri("/api/internal/inventory/reservations")
                    .body(new ReservationRequest(reservationId, productId, quantity))
                    .retrieve().body(ProductEnvelope.class);
            if (response == null || response.data() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid reservation response");
            }
            return response.data();
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient product quantity", e);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Products service is unavailable", e);
        }
    }

    @Observed(name = "marketplace.payment.stock.release", contextualName = "release-product-stock")
    public void releaseReservation(String reservationId, String productId, long quantity) {
        try {
            restClient.post().uri("/api/internal/inventory/reservations/release")
                    .body(new ReservationRequest(reservationId, productId, quantity))
                    .retrieve().toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            // Already released or confirmed; reservation endpoints are idempotent.
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProductEnvelope(ProductSnapshot data) { }

    private record ReservationRequest(String reservationId, String productId, long quantity) { }
}
