package com.example.products.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.products.dto.ProductRatingStatsDto;
import com.example.products.dto.RateProductDto;
import com.example.products.kafka.ProductEvents;
import com.example.products.models.Product;
import com.example.products.models.ProductRating;
import com.example.products.repositories.ProductRatingRepository;
import com.example.products.repositories.ProductRepository;

@Service
public class ProductRatingService {
    private final ProductRepository productRepository;
    private final ProductRatingRepository productRatingRepository;
    private final ProductEvents productEvents;

    public ProductRatingService(
            ProductRepository productRepository,
            ProductRatingRepository productRatingRepository,
            ProductEvents productEvents) {
        this.productRepository = productRepository;
        this.productRatingRepository = productRatingRepository;
        this.productEvents = productEvents;
    }

    public ProductRatingStatsDto getStats(UUID productId, String userId) {
        getProductOrThrow(productId);
        return buildStats(productId, userId);
    }

    public ProductRatingStatsDto rateProduct(UUID productId, String userId, RateProductDto dto) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required to rate a product");
        }

        Product product = getProductOrThrow(productId);
        if (userId.equals(product.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot rate your own product");
        }

        ProductRating rating = productRatingRepository.findByProductIdAndUserId(productId, userId)
                .orElseGet(() -> new ProductRating(productId, userId, dto.getStars()));
        rating.setStars(dto.getStars());
        productRatingRepository.save(rating);

        ProductRatingStatsDto stats = persistProductStats(product);
        productEvents.sendUpdateEvent(product);
        return stats;
    }

    public void deleteRatingsForProduct(UUID productId) {
        productRatingRepository.deleteByProductId(productId);
    }

    private Product getProductOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private ProductRatingStatsDto persistProductStats(Product product) {
        ProductRatingStatsDto stats = buildStats(product.getId(), null);
        product.setAverageRating(stats.average());
        product.setRatingCount(stats.count());
        product.setRatingBreakdown(stats.breakdown());
        productRepository.save(product);
        return stats;
    }

    private ProductRatingStatsDto buildStats(UUID productId, String userId) {
        List<ProductRating> ratings = productRatingRepository.findAllByProductId(productId);
        Map<Integer, Long> breakdown = emptyBreakdown();
        long totalScore = 0;

        for (ProductRating rating : ratings) {
            int stars = rating.getStars();
            if (stars < 1 || stars > 5) {
                continue;
            }
            breakdown.put(stars, breakdown.get(stars) + 1);
            totalScore += stars;
        }

        long count = breakdown.values().stream().mapToLong(Long::longValue).sum();
        double average = count == 0 ? 0 : Math.round(((double) totalScore / count) * 10.0) / 10.0;
        Integer myRating = null;
        if (userId != null && !userId.isBlank()) {
            myRating = productRatingRepository.findByProductIdAndUserId(productId, userId)
                    .map(ProductRating::getStars)
                    .orElse(null);
        }

        return new ProductRatingStatsDto(average, count, breakdown, myRating);
    }

    private Map<Integer, Long> emptyBreakdown() {
        Map<Integer, Long> breakdown = new LinkedHashMap<>();
        for (int stars = 5; stars >= 1; stars--) {
            breakdown.put(stars, 0L);
        }
        return breakdown;
    }
}
