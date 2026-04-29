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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.products.dto.CreateProdutDto;
import com.example.products.dto.UpdateProcutDto;
import com.example.products.models.Product;
import com.example.products.services.ProductService;

import com.example.shared.common.utils.ApiResponse;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    @PermitAll
    public ResponseEntity<ApiResponse<List<Product>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts()));
    }

    @GetMapping("/me")
    @PermitAll
    public ResponseEntity<ApiResponse<List<Product>>> getMyProducts(Authentication authentication) {
        String userId = extractUserId(authentication);

        return ResponseEntity.ok(ApiResponse.success(productService.getMyProducts(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable("id") UUID id) {
        Product product = this.productService.getProductById(id);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("", 404));
        }
        return ResponseEntity.ok(ApiResponse.success(product));
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

    private String extractUserId(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) return authentication.getName();
        if (principal instanceof String) return (String) principal;
        if (principal instanceof UserDetails) return ((UserDetails) principal).getUsername();
        if (principal instanceof Principal) return ((Principal) principal).getName();
        return authentication.getName();
    }

        if (!product.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not the owner of this product",
                            HttpStatus.FORBIDDEN));
        }

        Product updateProduct = this.productService.updateProduct(product, productDto);

        return ResponseEntity.ok(ApiResponse.success(updateProduct));
    }
}
