package com.example.products.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.example.products.dto.CreateProdutDto;
import com.example.products.dto.UpdateProcutDto;
import com.example.products.models.Product;
import com.example.products.services.ProductService;
import com.example.shared.common.utils.ApiResponse;

@SuppressWarnings("null")
class ProductControllerTest {
    private ProductService productService;
    private ProductController controller;

    private UUID productId;
    private String userId;
    private Product testProduct;
    private CreateProdutDto createDto;
    private UpdateProcutDto updateDto;

    @BeforeEach
    void setUp() {
        productService = new FakeProductService();
        controller = new ProductController(productService);

        productId = UUID.randomUUID();
        userId = "user-123";

        testProduct = new Product();
        testProduct.setId(productId);
        testProduct.setName("Test Product");
        testProduct.setDescription("A test product description");
        testProduct.setPrice(29.99);
        testProduct.setQuantity(10);
        testProduct.setUserId(userId);

        createDto = new CreateProdutDto();
        createDto.setName("New Product");
        createDto.setDescription("New product description");
        createDto.setPrice(49.99);

        updateDto = new UpdateProcutDto();
        updateDto.setName("Updated Product");
        updateDto.setDescription("Updated description");
        updateDto.setPrice(39.99);
    }

    private Authentication authentication(String principal) {
        return new Authentication() {
            @Override
            public String getName() { return principal; }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}

            @Override
            public boolean isAuthenticated() { return true; }

            @Override
            public Object getPrincipal() { return principal; }

            @Override
            public Object getDetails() { return null; }

            @Override
            public Object getCredentials() { return null; }

            @Override
            public java.util.Collection<org.springframework.security.core.GrantedAuthority> getAuthorities() { return java.util.List.of(); }
        };
    }

    @Test
    void getProductsReturnsApiResponse() {
        FakeProductService fake = (FakeProductService) productService;
        fake.allProducts = List.of(testProduct);

        ResponseEntity<ApiResponse<List<Product>>> response = controller.getProducts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().data().size());
        assertEquals("Test Product", response.getBody().data().get(0).getName());
        assertTrue(fake.getAllProductsCalled);
    }

    @Test
    void getMyProductsUsesAuthenticatedPrincipal() {
        FakeProductService fake = (FakeProductService) productService;
        fake.myProducts = List.of(testProduct);

        ResponseEntity<ApiResponse<List<Product>>> response = controller.getMyProducts(authentication(userId));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("user-123", response.getBody().data().get(0).getUserId());
        assertTrue(fake.getMyProductsCalled && userId.equals(fake.lastGetMyProductsUserId));
    }

    @Test
    void getProductByIdSuccess() {
        FakeProductService fake = (FakeProductService) productService;
        fake.productById = testProduct;

        ResponseEntity<ApiResponse<Product>> response = controller.getProduct(productId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(productId, response.getBody().data().getId());
        assertTrue(fake.getProductByIdCalled && productId.equals(fake.lastGetProductId));
    }

    @Test
    void getProductByIdReturnsNotFoundWhenServiceReturnsNull() {
        FakeProductService fake = (FakeProductService) productService;
        fake.productById = null;

        ResponseEntity<ApiResponse<Product>> response = controller.getProduct(productId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().success());
    }

    @Test
    void createProductReturnsCreatedResponse() {
        Product createdProduct = new Product(createDto, userId);
        createdProduct.setId(productId);
        FakeProductService fake = (FakeProductService) productService;
        fake.createdProduct = createdProduct;

        ResponseEntity<ApiResponse<Product>> response = controller.createProduct(createDto, authentication(userId));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("New Product", response.getBody().data().getName());
        assertTrue(userId.equals(fake.lastCreateUserId));
    }

    @Test
    void deleteProductReturnsNoContentForOwner() {
        FakeProductService fake = (FakeProductService) productService;
        fake.productById = testProduct;

        ResponseEntity<ApiResponse<Product>> response = controller.deleteProduct(productId, authentication(userId));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(fake.deleteCalled && productId.equals(fake.lastDeletedId));
    }

    @Test
    void deleteProductReturnsForbiddenForDifferentUser() {
        FakeProductService fake = (FakeProductService) productService;
        fake.productById = testProduct;

        ResponseEntity<ApiResponse<Product>> response = controller.deleteProduct(productId, authentication("different-user"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().success());
    }

    @Test
    void updateProductReturnsOkForOwner() {
        Product updatedProduct = new Product();
        updatedProduct.setId(productId);
        updatedProduct.setName("Updated Product");
        updatedProduct.setDescription("Updated description");
        updatedProduct.setPrice(39.99);
        updatedProduct.setUserId(userId);

        FakeProductService fake = (FakeProductService) productService;
        fake.productById = testProduct;
        fake.updatedProduct = updatedProduct;

        ResponseEntity<ApiResponse<Product>> response = controller.updateProduct(productId, updateDto, authentication(userId));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Product", response.getBody().data().getName());
        assertTrue(fake.updateCalled && fake.lastUpdateProduct == testProduct && fake.lastUpdateDto == updateDto);
    }

    @Test
    void updateProductReturnsForbiddenForDifferentUser() {
        FakeProductService fake = (FakeProductService) productService;
        fake.productById = testProduct;

        ResponseEntity<ApiResponse<Product>> response = controller.updateProduct(productId, updateDto, authentication("different-user"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().success());
    }

    // Simple fake to avoid Mockito inline mocking which fails on newer JVMs
    static class FakeProductService extends ProductService {
        List<Product> allProducts = List.of();
        List<Product> myProducts = List.of();
        Product productById = null;
        Product createdProduct = null;
        boolean getAllProductsCalled = false;
        boolean getMyProductsCalled = false;
        String lastGetMyProductsUserId = null;
        boolean getProductByIdCalled = false;
        java.util.UUID lastGetProductId = null;
        boolean deleteCalled = false;
        java.util.UUID lastDeletedId = null;
        Product updatedProduct = null;
        boolean updateCalled = false;
        Product lastUpdateProduct = null;
        UpdateProcutDto lastUpdateDto = null;
        String lastCreateUserId = null;

        public FakeProductService() {
            super(null, null, null, null);
        }

        @Override
        public List<Product> getAllProducts() {
            this.getAllProductsCalled = true;
            return this.allProducts;
        }

        @Override
        public List<Product> getMyProducts(String userId) {
            this.getMyProductsCalled = true;
            this.lastGetMyProductsUserId = userId;
            return this.myProducts;
        }

        @Override
        public Product getProductById(java.util.UUID id) {
            this.getProductByIdCalled = true;
            this.lastGetProductId = id;
            return this.productById;
        }

        @Override
        public Product createProduct(CreateProdutDto productDto, String userId) {
            this.lastCreateUserId = userId;
            return this.createdProduct;
        }

        @Override
        public void deleteProduct(java.util.UUID id) {
            this.deleteCalled = true;
            this.lastDeletedId = id;
        }

        @Override
        public Product updateProduct(Product product, UpdateProcutDto productDto) {
            this.updateCalled = true;
            this.lastUpdateProduct = product;
            this.lastUpdateDto = productDto;
            return this.updatedProduct;
        }
    }
}
