package com.example.products.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.products.dto.ProductRatingStatsDto;
import com.example.products.dto.RateProductDto;
import com.example.products.kafka.ProductEvents;
import com.example.products.models.Product;
import com.example.products.models.ProductRating;
import com.example.products.repositories.ProductRatingRepository;
import com.example.products.repositories.ProductRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ProductRatingServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductRatingRepository productRatingRepository;

    @Mock
    private ProductEvents productEvents;

    private ProductRatingService productRatingService;

    @BeforeEach
    void setUp() {
        productRatingService = new ProductRatingService(productRepository, productRatingRepository, productEvents);
    }

    @Test
    void rateProductCreatesRatingAndUpdatesProductStats() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setUserId("seller-1");

        RateProductDto dto = new RateProductDto();
        dto.setStars(5);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productRatingRepository.findByProductIdAndUserId(productId, "buyer-1")).thenReturn(Optional.empty());
        when(productRatingRepository.findAllByProductId(productId))
                .thenReturn(List.of(
                        new ProductRating(productId, "buyer-1", 5),
                        new ProductRating(productId, "buyer-2", 4)));

        ProductRatingStatsDto stats = productRatingService.rateProduct(productId, "buyer-1", dto);

        assertEquals(4.5, stats.average());
        assertEquals(2, stats.count());
        assertEquals(1L, stats.breakdown().get(5));
        assertEquals(1L, stats.breakdown().get(4));
        assertEquals(4.5, product.getAverageRating());
        assertEquals(2, product.getRatingCount());
        verify(productRatingRepository).save(org.mockito.ArgumentMatchers.any(ProductRating.class));
        verify(productRepository).save(product);
        verify(productEvents).sendUpdateEvent(product);
    }

    @Test
    void rateProductRejectsProductOwner() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setUserId("seller-1");

        RateProductDto dto = new RateProductDto();
        dto.setStars(5);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productRatingService.rateProduct(productId, "seller-1", dto));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
