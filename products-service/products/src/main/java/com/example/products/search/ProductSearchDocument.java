package com.example.products.search;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.example.products.models.Product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchDocument {
    private String id;
    private String name;
    private String description;
    private String category;
    private String condition;
    private double price;
    private List<String> images;
    private double averageRating;
    private long ratingCount;
    private String userId;
    private String createdAt;

    public static ProductSearchDocument fromProduct(Product product) {
        return new ProductSearchDocument(
                product.getId().toString(),
                product.getName(),
                product.getDescription(),
                normalizeCategory(product.getCategory()),
                normalizeCondition(product.getCondition()),
                product.getPrice(),
                product.getImages().stream().map(UUID::toString).toList(),
                product.getAverageRating(),
                product.getRatingCount(),
                product.getUserId(),
                product.getCreatedAt() == null ? null : product.getCreatedAt().toString());
    }

    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return null;
        }
        return condition.trim().toLowerCase(Locale.ROOT);
    }
}
