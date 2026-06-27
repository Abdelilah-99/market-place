package com.example.products.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.products.models.Product;
import com.example.products.search.ProductSearchDocument;
import com.example.products.search.events.ProductSearchSyncEvent;
import com.example.shared.common.kafka.EventNames;

import com.example.shared.common.kafka.dtos.products.*;

@Service
public class ProductEvents {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ProductEvents(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendCreateEvent(Product p) {
        KafkaProductCreatedEvent event = new KafkaProductCreatedEvent(p.getId(), p.getUserId());
        kafkaTemplate.send(EventNames.CREATE_PRODUCT_EVENT_NAME, null, event);
        sendSearchUpsertEvent(p);
    }

    public void sendUpdateEvent(Product p) {
        kafkaTemplate.send(EventNames.UPDATE_PRODUCT_EVENT_NAME, null,
                new KafkaProductUpdatedEvent(p.getId(), p.getUserId()));
        sendSearchUpsertEvent(p);
    }

    public void sendRemoveEvent(Product p) {
        KafkaProductRemovedEvent event = new KafkaProductRemovedEvent(p.getId());
        kafkaTemplate.send(EventNames.REMOVE_PRODUCT_EVENT_NAME, null, event);
        kafkaTemplate.send(EventNames.PRODUCT_SEARCH_SYNC_EVENT_NAME, null,
                new ProductSearchSyncEvent(ProductSearchSyncEvent.DELETE, ProductSearchDocument.fromProduct(p)));
    }

    private void sendSearchUpsertEvent(Product p) {
        kafkaTemplate.send(EventNames.PRODUCT_SEARCH_SYNC_EVENT_NAME, null,
                new ProductSearchSyncEvent(ProductSearchSyncEvent.UPSERT, ProductSearchDocument.fromProduct(p)));
    }
}
