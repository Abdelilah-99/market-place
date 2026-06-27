package com.example.products.search.dto;

import java.util.List;

import com.example.products.search.ProductSearchDocument;

public record ProductSearchResponse(
        List<ProductSearchDocument> items,
        long total,
        int page,
        int size,
        int totalPages) {
}
