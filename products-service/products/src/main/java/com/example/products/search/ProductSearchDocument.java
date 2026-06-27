package com.example.products.search;

import java.util.List;
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
    private double price;
    private List<String> images;
    private String userId;
    private String createdAt;

    public static ProductSearchDocument fromProduct(Product product) {
        return new ProductSearchDocument(
                product.getId().toString(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getImages().stream().map(UUID::toString).toList(),
                product.getUserId(),
                product.getCreatedAt() == null ? null : product.getCreatedAt().toString());
    }
}
