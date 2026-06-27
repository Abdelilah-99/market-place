package com.example.products.services;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.products.dto.CreateProdutDto;
import com.example.products.dto.UpdateProcutDto;
import com.example.products.kafka.ProductEvents;
import com.example.products.kafka.MediaEvents;
import com.example.products.models.Product;
import com.example.products.repositories.ProductRepository;
import com.example.products.search.ProductSearchService;
import com.example.products.search.dto.ProductSearchResponse;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductEvents productEvents;
    private final MediaEvents mediaEvents;
    private final ProductSearchService productSearchService;

    public ProductService(ProductRepository productRepository,
            ProductEvents productEvents,
            MediaEvents mediaEvents,
            ProductSearchService productSearchService) {
        this.productRepository = productRepository;
        this.productEvents = productEvents;
        this.mediaEvents = mediaEvents;
        this.productSearchService = productSearchService;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Product> getMyProducts(String userId) {
        return productRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    public ProductSearchResponse searchProducts(
            String q,
            String category,
            Double minPrice,
            Double maxPrice,
            int page,
            int size,
            String sort) {
        return productSearchService.search(q, category, minPrice, maxPrice, page, size, sort);
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public Product createProduct(CreateProdutDto productDto, String userId) {
        Product product = new Product(productDto, userId);

        // 1 Save first (source of truth)
        Product saved = productRepository.save(product);

        // 2 Then emit events
        productEvents.sendCreateEvent(saved);
        mediaEvents.confimImageEvent(saved.getImage());

        return saved;
    }

    public void deleteProduct(UUID id) {
        Product product = getProductById(id);

        productRepository.deleteById(id);
        productEvents.sendRemoveEvent(product);
        mediaEvents.deleteImageEvent(product.getImage());
    }

    public Product updateProduct(Product product, UpdateProcutDto productDto) {

        if (productDto.getName() != null && !productDto.getName().isBlank()) {
            product.setName(productDto.getName());
        }

        if (productDto.getDescription() != null && !productDto.getDescription().isBlank()) {
            product.setDescription(productDto.getDescription());
        }

        if (productDto.getCategory() != null && !productDto.getCategory().isBlank()) {
            product.setCategory(productDto.getCategory());
        }

        if (productDto.getPrice() != null && productDto.getPrice() > 0) {
            product.setPrice(productDto.getPrice());
        }

        if (productDto.getImage() != null &&
                !productDto.getImage().equals(product.getImage())) {

            // confirm new image first
            mediaEvents.confimImageEvent(productDto.getImage());

            UUID oldImage = product.getImage();
            product.setImage(productDto.getImage());

            Product saved = productRepository.save(product);

            // delete old image after success
            mediaEvents.deleteImageEvent(oldImage);
            productEvents.sendUpdateEvent(saved);

            return saved;
        }

        Product saved = productRepository.save(product);
        productEvents.sendUpdateEvent(saved);
        return saved;
    }
}
