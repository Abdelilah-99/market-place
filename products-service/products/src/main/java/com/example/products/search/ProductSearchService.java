package com.example.products.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.example.products.models.Product;
import com.example.products.repositories.ProductRepository;
import com.example.products.search.dto.ProductSearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class ProductSearchService {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;
    private final String indexName;

    public ProductSearchService(
            ProductRepository productRepository,
            ObjectMapper objectMapper,
            @Value("${search.opensearch.host}") String host,
            @Value("${search.opensearch.port}") int port,
            @Value("${search.opensearch.scheme}") String scheme,
            @Value("${search.products.index}") String indexName) {
        this.restClient = RestClient.builder()
                .baseUrl(scheme + "://" + host + ":" + port)
                .build();
        this.objectMapper = objectMapper;
        this.productRepository = productRepository;
        this.indexName = indexName;
    }

    @PostConstruct
    public void ensureIndexExists() throws IOException {
        if (indexExists()) {
            return;
        }

        restClient.put()
                .uri("/{index}", indexName)
                .body("""
                        {
                            "settings": {
                                "analysis": {
                                    "analyzer": {
                                        "product_text": {
                                            "type": "standard",
                                            "stopwords": "_english_"
                                        }
                                    }
                                }
                            },
                            "mappings": {
                                "properties": {
                                    "id": { "type": "keyword" },
                                    "name": { "type": "text", "analyzer": "product_text", "fields": { "keyword": { "type": "keyword" } } },
                                    "description": { "type": "text", "analyzer": "product_text" },
                                    "category": { "type": "keyword" },
                                    "price": { "type": "double" },
                                    "images": { "type": "keyword" },
                                    "userId": { "type": "keyword" },
                                    "createdAt": { "type": "date" }
                                }
                            }
                        }
                        """)
                .retrieve()
                .toBodilessEntity();
    }

    public void indexProduct(Product product) {
        indexDocument(ProductSearchDocument.fromProduct(product));
    }

    public void indexDocument(ProductSearchDocument document) {
        try {
            restClient.put()
                    .uri("/{index}/_doc/{id}", indexName, document.getId())
                    .body(document)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to index product " + document.getId(), e);
        }
    }

    public void deleteProduct(String productId) {
        try {
            restClient.delete()
                    .uri("/{index}/_doc/{id}", indexName, productId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            // Deleting an already-missing search document should be idempotent.
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to delete product from search index " + productId, e);
        }
    }

    public ProductSearchResponse search(
            String q,
            String category,
            Double minPrice,
            Double maxPrice,
            int page,
            int size,
            String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("from", safePage * safeSize);
            request.put("size", safeSize);
            request.put("query", buildQuery(q, category, minPrice, maxPrice));
            request.put("sort", buildSort(sort));

            JsonNode response = restClient.post()
                    .uri("/{index}/_search", indexName)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode hits = response.path("hits");
            List<ProductSearchDocument> items = new ArrayList<>();
            for (JsonNode hit : hits.path("hits")) {
                items.add(objectMapper.treeToValue(hit.path("_source"), ProductSearchDocument.class));
            }

            long total = hits.path("total").path("value").asLong(items.size());
            int totalPages = safeSize == 0 ? 0 : (int) Math.ceil((double) total / safeSize);

            return new ProductSearchResponse(items, total, safePage, safeSize, totalPages);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to search products", e);
        }
    }

    public long reindexAllProducts() {
        List<Product> products = productRepository.findAll();
        products.forEach(this::indexProduct);
        return products.size();
    }

    private Map<String, Object> buildQuery(String q, String category, Double minPrice, Double maxPrice) {
        List<Object> must = new ArrayList<>();
        List<Object> filter = new ArrayList<>();

        if (q == null || q.isBlank()) {
            must.add(Map.of("match_all", Map.of()));
        } else {
            must.add(Map.of("multi_match", Map.of(
                    "query", q,
                    "fields", List.of("name^3", "description", "category"),
                    "fuzziness", "AUTO")));
        }

        if (category != null && !category.isBlank()) {
            filter.add(Map.of("term", Map.of("category", category)));
        }

        if (minPrice != null || maxPrice != null) {
            Map<String, Object> priceRange = new LinkedHashMap<>();
            if (minPrice != null) {
                priceRange.put("gte", minPrice);
            }
            if (maxPrice != null) {
                priceRange.put("lte", maxPrice);
            }
            filter.add(Map.of("range", Map.of("price", priceRange)));
        }

        Map<String, Object> bool = new LinkedHashMap<>();
        bool.put("must", must);
        bool.put("filter", filter);
        return Map.of("bool", bool);
    }

    private List<Object> buildSort(String sort) {
        String normalized = sort == null ? "newest" : sort.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "price_asc" -> List.of(Map.of("price", Map.of("order", "asc")));
            case "price_desc" -> List.of(Map.of("price", Map.of("order", "desc")));
            case "newest" -> List.of(Map.of("createdAt", Map.of("order", "desc")));
            default -> List.of(Map.of("_score", Map.of("order", "desc")));
        };
    }

    private boolean indexExists() {
        try {
            restClient.head()
                    .uri("/{index}", indexName)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw e;
        }
    }
}
