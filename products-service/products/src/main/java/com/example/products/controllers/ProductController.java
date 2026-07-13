package com.example.products.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import java.security.Principal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.products.dto.CreateProdutDto;
import com.example.products.dto.PageResponseDto;
import com.example.products.dto.ProductRatingStatsDto;
import com.example.products.dto.RateProductDto;
import com.example.products.dto.UpdateProcutDto;
import com.example.products.models.Product;
import com.example.products.search.dto.ProductSearchResponse;
import com.example.products.services.ProductRatingService;
import com.example.products.services.ProductService;

import com.example.shared.common.utils.ApiResponse;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    private final ProductRatingService productRatingService;

    @Autowired
    public ProductController(ProductService productService, ProductRatingService productRatingService) {
        this.productService = productService;
        this.productRatingService = productRatingService;
    }

    @GetMapping("/")
    @PermitAll
    public ResponseEntity<ApiResponse<List<Product>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts()));
    }

    @GetMapping("/page")
    @PermitAll
    public ResponseEntity<ApiResponse<PageResponseDto<Product>>> getProductsPage(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsPage(page, size)));
    }

    @GetMapping("/search")
    @PermitAll
    public ResponseEntity<ApiResponse<ProductSearchResponse>> searchProducts(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "minPrice", required = false) Double minPrice,
            @RequestParam(name = "maxPrice", required = false) Double maxPrice,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "sort", defaultValue = "newest") String sort) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.searchProducts(q, category, minPrice, maxPrice, page, size, sort)));
    }

    @GetMapping("/category/{category}")
    @PermitAll
    public ResponseEntity<ApiResponse<List<Product>>> getProductsByCategory(
            @PathVariable("category") String category) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByCategory(category)));
    }

    @GetMapping("/me")
    @PermitAll
    public ResponseEntity<ApiResponse<List<Product>>> getMyProducts(Authentication authentication) {
        String userId = extractUserId(authentication);

        return ResponseEntity.ok(ApiResponse.success(productService.getMyProducts(userId)));
    }

    @GetMapping("/me/page")
    public ResponseEntity<ApiResponse<PageResponseDto<Product>>> getMyProductsPage(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        String userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(productService.getMyProductsPage(userId, page, size)));
    }

    @GetMapping("/user/{userId}")
    @PermitAll
    public ResponseEntity<ApiResponse<PageResponseDto<Product>>> getProductsByUser(
            @PathVariable String userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByUser(userId, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable("id") UUID id) {
        Product product = this.productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("", 404));
        }
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/{id}/ratings")
    @PermitAll
    public ResponseEntity<ApiResponse<ProductRatingStatsDto>> getProductRatings(
            @PathVariable("id") UUID id,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                productRatingService.getStats(id, extractOptionalUserId(authentication))));
    }

    @PostMapping("/{id}/ratings")
    public ResponseEntity<ApiResponse<ProductRatingStatsDto>> rateProduct(
            @PathVariable("id") UUID id,
            @RequestBody @Valid RateProductDto ratingDto,
            Authentication authentication) {
        String userId = extractUserId(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                productRatingService.rateProduct(id, userId, ratingDto)));
    }

    @PostMapping("/")
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @RequestBody @Valid CreateProdutDto productDto,
            Authentication authentication) {

        String userId = extractUserId(authentication);

        Product createdProduct = this.productService.createProduct(productDto, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdProduct, HttpStatus.CREATED.value()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> deleteProduct(@PathVariable("id") UUID id,
            Authentication authentication) {

        Product product = this.productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Product not found", HttpStatus.NOT_FOUND));
        }

        String userId = extractUserId(authentication);

        if (!product.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not the owner of this product",
                            HttpStatus.FORBIDDEN));
        }

        this.productService.deleteProduct(id);

        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.successStatus(HttpStatus.NO_CONTENT.value()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable("id") UUID id,
            @RequestBody @Valid UpdateProcutDto productDto,
            Authentication authentication) {

        Product product = this.productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Product not found", HttpStatus.NOT_FOUND));
        }

        String userId = extractUserId(authentication);

        if (!product.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not the owner of this product",
                            HttpStatus.FORBIDDEN));
        }

        Product updateProduct = this.productService.updateProduct(product, productDto);

        return ResponseEntity.ok(ApiResponse.success(updateProduct));
    }

    private String extractUserId(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        Object principal = authentication.getPrincipal();
        if (principal == null)
            return authentication.getName();
        if (principal instanceof String)
            return (String) principal;
        if (principal instanceof UserDetails)
            return ((UserDetails) principal).getUsername();
        if (principal instanceof Principal)
            return ((Principal) principal).getName();
        return authentication.getName();
    }

    private String extractOptionalUserId(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal)) {
            return null;
        }
        if (principal instanceof String)
            return (String) principal;
        if (principal instanceof UserDetails)
            return ((UserDetails) principal).getUsername();
        if (principal instanceof Principal)
            return ((Principal) principal).getName();
        return authentication.getName();
    }
}
