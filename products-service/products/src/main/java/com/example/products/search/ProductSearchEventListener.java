package com.example.products.search;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.products.search.events.ProductSearchSyncEvent;

@Service
public class ProductSearchEventListener {
    private final ProductSearchService productSearchService;

    public ProductSearchEventListener(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @KafkaListener(topics = "product-search-sync-events", groupId = "products-search-indexer")
    public void syncProductIndex(ProductSearchSyncEvent event) {
        if (event == null || event.product() == null) {
            return;
        }

        if (ProductSearchSyncEvent.DELETE.equals(event.action())) {
            productSearchService.deleteProduct(event.product().getId());
            return;
        }

        productSearchService.indexDocument(event.product());
    }
}
