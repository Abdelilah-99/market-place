package com.example.products.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.products.dto.PageResponseDto;
import com.example.products.models.Product;
import com.example.products.search.ProductSearchService;
import com.example.products.services.ProductService;
import com.example.shared.common.utils.ApiResponse;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {
    private final ProductSearchService productSearchService;
    private final ProductService productService;

    public AdminProductController(ProductSearchService productSearchService, ProductService productService) {
        this.productSearchService = productSearchService;
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDto<Product>>> getProducts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsPage(page, size)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteProduct(@PathVariable("id") UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", id.toString())));
    }

    @PostMapping("/reindex-search")
    public ResponseEntity<ApiResponse<Map<String, Long>>> reindexSearch() {
        long indexed = productSearchService.reindexAllProducts();
        return ResponseEntity.ok(ApiResponse.success(Map.of("indexed", indexed)));
    }
}
