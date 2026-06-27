package com.example.products.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.products.search.ProductSearchService;
import com.example.shared.common.utils.ApiResponse;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductSearchService productSearchService;

    public AdminProductController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @PostMapping("/reindex-search")
    public ResponseEntity<ApiResponse<Map<String, Long>>> reindexSearch() {
        long indexed = productSearchService.reindexAllProducts();
        return ResponseEntity.ok(ApiResponse.success(Map.of("indexed", indexed)));
    }
}
