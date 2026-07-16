package com.example.products.services;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.server.ResponseStatusException;

import com.example.products.dto.CreateProdutDto;
import com.example.products.dto.PageResponseDto;
import com.example.products.dto.UpdateProcutDto;
import com.example.products.kafka.ProductEvents;
import com.example.products.kafka.MediaEvents;
import com.example.products.models.Product;
import com.example.products.models.StockReservation;
import com.example.products.repositories.ProductRatingRepository;
import com.example.products.repositories.ProductRepository;
import com.example.products.search.ProductSearchService;
import com.example.products.search.dto.ProductSearchResponse;
import io.micrometer.observation.annotation.Observed;

@Service
public class ProductService {
    private static final int LEGACY_LIST_LIMIT = 50;

    private final ProductRepository productRepository;
    private final ProductEvents productEvents;
    private final MediaEvents mediaEvents;
    private final ProductSearchService productSearchService;
    private final ProductRatingRepository productRatingRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public ProductService(ProductRepository productRepository,
            ProductEvents productEvents,
            MediaEvents mediaEvents,
            ProductSearchService productSearchService,
            ProductRatingRepository productRatingRepository,
            MongoTemplate mongoTemplate) {
        this.productRepository = productRepository;
        this.productEvents = productEvents;
        this.mediaEvents = mediaEvents;
        this.productSearchService = productSearchService;
        this.productRatingRepository = productRatingRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public ProductService(ProductRepository productRepository,
            ProductEvents productEvents,
            MediaEvents mediaEvents,
            ProductSearchService productSearchService,
            ProductRatingRepository productRatingRepository) {
        this(productRepository, productEvents, mediaEvents, productSearchService,
                productRatingRepository, null);
    }

    public List<Product> getAllProducts() {
        return getProductsPage(0, LEGACY_LIST_LIMIT).items();
    }

    public PageResponseDto<Product> getProductsPage(int page, int size) {
        Page<Product> products = productRepository.findAll(pageRequest(page, size));
        return toPageResponse(products);
    }

    public List<Product> getMyProducts(String userId) {
        return getMyProductsPage(userId, 0, LEGACY_LIST_LIMIT).items();
    }

    public PageResponseDto<Product> getMyProductsPage(String userId, int page, int size) {
        Page<Product> products = productRepository.findAllByUserId(userId, pageRequest(page, size));
        return toPageResponse(products);
    }

    public PageResponseDto<Product> getProductsByUser(String userId, int page, int size) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        Page<Product> products = productRepository.findAllByUserId(userId, pageRequest(page, size));
        return toPageResponse(products);
    }

    public List<Product> getProductsByCategory(String category) {
        String normalizedCategory = normalizeCategory(category);
        if (normalizedCategory == null) {
            return List.of();
        }
        return productRepository.findAllByCategoryIgnoreCase(normalizedCategory, pageRequest(0, LEGACY_LIST_LIMIT))
                .getContent();
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
        mediaEvents.confirmImageEvents(saved.getImages());

        return saved;
    }

    public void deleteProduct(UUID id) {
        Product product = getProductById(id);

        productRepository.deleteById(id);
        productRatingRepository.deleteByProductId(id);
        productEvents.sendRemoveEvent(product);
        mediaEvents.deleteImageEvents(product.getImages());
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

        if (productDto.getCondition() != null && !productDto.getCondition().isBlank()) {
            product.setCondition(productDto.getCondition());
        }

        if (productDto.getPrice() != null && productDto.getPrice() > 0) {
            product.setPrice(productDto.getPrice());
        }

        List<UUID> oldImages = product.getImages();
        List<UUID> requestedImages = normalizeImages(productDto.getImage(), productDto.getImages());
        if (!requestedImages.isEmpty() && !requestedImages.equals(oldImages)) {
            product.setImage(requestedImages.get(0));
            product.setImages(requestedImages);

            Product saved = productRepository.save(product);

            mediaEvents.confirmImageEvents(requestedImages);
            mediaEvents.deleteImageEvents(imagesToDelete(oldImages, requestedImages));
            productEvents.sendUpdateEvent(saved);

            return saved;
        }

        Product saved = productRepository.save(product);
        productEvents.sendUpdateEvent(saved);
        return saved;
    }

    public boolean decrementQuantity(String eventId, String productId, long quantity) {
        if (eventId == null || eventId.isBlank() || quantity <= 0) {
            return false;
        }
        UUID id;
        try {
            id = UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            return false;
        }
        Query query = Query.query(Criteria.where("_id").is(id)
                .and("quantity").gte(quantity)
                .and("processedSaleEventIds").ne(eventId));
        Update update = new Update().inc("quantity", -quantity).push("processedSaleEventIds", eventId);
        Product updated = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Product.class);
        if (updated != null) {
            productEvents.sendUpdateEvent(updated);
            return true;
        }
        return false;
    }

    @Observed(name = "marketplace.inventory.stock.reserve", contextualName = "reserve-stock")
    public Product reserveStock(String reservationId, String productId, long quantity) {
        UUID id = parseProductId(productId);
        if (reservationId == null || reservationId.isBlank() || quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reservation");
        }
        Query query = Query.query(Criteria.where("_id").is(id)
                .and("quantity").gte(quantity)
                .and("stockReservations.reservationId").ne(reservationId));
        Update update = new Update().inc("quantity", -quantity)
                .push("stockReservations", new StockReservation(reservationId, quantity));
        Product reserved = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Product.class);
        if (reserved == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Insufficient stock or reservation already exists");
        }
        productEvents.sendUpdateEvent(reserved);
        return reserved;
    }

    @Observed(name = "marketplace.inventory.stock.confirm", contextualName = "confirm-stock-reservation")
    public boolean confirmReservation(String reservationId, String productId, long quantity) {
        if (reservationId == null || reservationId.isBlank() || quantity <= 0) {
            return false;
        }
        UUID id = parseProductId(productId);
        Query query = Query.query(Criteria.where("_id").is(id)
                .and("stockReservations").elemMatch(Criteria.where("reservationId").is(reservationId)
                        .and("quantity").is(quantity)));
        Product confirmed = mongoTemplate.findAndModify(query,
                new Update().pull("stockReservations", Query.query(
                        Criteria.where("reservationId").is(reservationId)).getQueryObject()),
                FindAndModifyOptions.options().returnNew(true), Product.class);
        return confirmed != null;
    }

    @Observed(name = "marketplace.inventory.stock.release", contextualName = "release-stock-reservation")
    public boolean releaseReservation(String reservationId, String productId, long quantity) {
        if (reservationId == null || reservationId.isBlank() || quantity <= 0) {
            return false;
        }
        UUID id = parseProductId(productId);
        Query query = Query.query(Criteria.where("_id").is(id)
                .and("stockReservations").elemMatch(Criteria.where("reservationId").is(reservationId)
                        .and("quantity").is(quantity)));
        Update update = new Update().inc("quantity", quantity)
                .pull("stockReservations", Query.query(
                        Criteria.where("reservationId").is(reservationId)).getQueryObject());
        Product released = mongoTemplate.findAndModify(query, update,
                FindAndModifyOptions.options().returnNew(true), Product.class);
        if (released != null) {
            productEvents.sendUpdateEvent(released);
            return true;
        }
        return false;
    }

    private UUID parseProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product id");
        }
        try {
            return UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid product id", e);
        }
    }

    private List<UUID> normalizeImages(UUID primaryImage, List<UUID> images) {
        Set<UUID> normalized = new LinkedHashSet<>();
        if (images != null) {
            images.stream().filter(image -> image != null).forEach(normalized::add);
        }
        if (normalized.isEmpty() && primaryImage != null) {
            normalized.add(primaryImage);
        }
        return new ArrayList<>(normalized);
    }

    private List<UUID> imagesToDelete(List<UUID> oldImages, List<UUID> newImages) {
        if (oldImages == null || oldImages.isEmpty()) {
            return List.of();
        }
        return oldImages.stream()
                .filter(image -> image != null && (newImages == null || !newImages.contains(image)))
                .toList();
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.trim().toLowerCase(Locale.ROOT);
    }

    private PageRequest pageRequest(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponseDto<Product> toPageResponse(Page<Product> page) {
        return new PageResponseDto<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.hasNext());
    }
}
