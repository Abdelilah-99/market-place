package com.example.products.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.example.products.dto.CreateProdutDto;
import com.example.products.dto.UpdateProcutDto;
import com.example.products.kafka.MediaEvents;
import com.example.products.kafka.ProductEvents;
import com.example.products.models.Product;
import com.example.products.repositories.ProductRatingRepository;
import com.example.products.repositories.ProductRepository;
import com.example.products.search.ProductSearchService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ProductEvents productEvents;
    @Mock
    private MediaEvents mediaEvents;
    @Mock
    private ProductSearchService productSearchService;
    @Mock
    private ProductRatingRepository productRatingRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        // productEvents = new ProductEvents(kafkaTemplate);
        // mediaEvents = new MediaEvents(kafkaTemplate);
        productService = new ProductService(productRepository, productEvents, mediaEvents, productSearchService,
                productRatingRepository);
    }

    @Test
    void getProductByIdThrowsNotFoundWhenMissing() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.getProductById(productId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void createProductSavesAndPublishesEvents() {
        UUID imageId = UUID.randomUUID();
        CreateProdutDto dto = new CreateProdutDto();
        dto.setName("Book");
        dto.setDescription("Useful product");
        dto.setPrice(22.5);
        dto.setImage(imageId);

        Product saved = new Product(dto, "u-1");
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        Product result = productService.createProduct(dto, "u-1");

        assertEquals(saved, result);
        verify(productRepository).save(any(Product.class));
        verify(productEvents).sendCreateEvent(saved);
        verify(mediaEvents).confirmImageEvents(List.of(imageId));
    }

    @Test
    void getProductsByCategoryNormalizesCategoryBeforeQueryingRepository() {
        Product product = new Product();
        product.setCategory("electronics");
        when(productRepository.findAllByCategoryIgnoreCase(org.mockito.ArgumentMatchers.eq("electronics"),
                any(Pageable.class))).thenReturn(new PageImpl<>(List.of(product)));

        List<Product> result = productService.getProductsByCategory(" Electronics ");

        assertEquals(1, result.size());
        assertEquals("electronics", result.get(0).getCategory());
        verify(productRepository).findAllByCategoryIgnoreCase(org.mockito.ArgumentMatchers.eq("electronics"),
                any(Pageable.class));
    }

    @Test
    void getProductsByCategoryReturnsEmptyListForBlankCategory() {
        List<Product> result = productService.getProductsByCategory(" ");

        assertEquals(0, result.size());
    }

    @Test
    void deleteProductDeletesRepositoryAndImage() {
        UUID productId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        Product product = new Product();
        product.setImage(imageId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.deleteProduct(productId);

        verify(productRepository).deleteById(productId);
        verify(productRatingRepository).deleteByProductId(productId);
        verify(mediaEvents).deleteImageEvents(List.of(imageId));
    }

    @Test
    void updateProductWithNewImageConfirmsNewAndDeletesOld() {
        UUID oldImage = UUID.randomUUID();
        UUID newImage = UUID.randomUUID();

        Product product = new Product();
        product.setImage(oldImage);
        product.setName("Old");

        UpdateProcutDto dto = new UpdateProcutDto();
        dto.setName("New Name");
        dto.setImage(newImage);

        when(productRepository.save(product)).thenReturn(product);

        Product result = productService.updateProduct(product, dto);

        assertEquals(product, result);
        assertEquals("New Name", product.getName());
        assertEquals(newImage, product.getImage());
        verify(mediaEvents).confirmImageEvents(List.of(newImage));
        verify(mediaEvents).deleteImageEvents(List.of(oldImage));
        verify(productRepository).save(product);
    }
}
