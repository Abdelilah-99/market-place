package com.example.products.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.products.models.Product;
import com.example.shared.common.kafka.dtos.media.KafkaConfirmImageEvent;
import com.example.shared.common.kafka.dtos.products.KafkaProductCreatedEvent;
import com.example.shared.common.kafka.dtos.products.KafkaProductRemovedEvent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class ProductsServiceKafkaProducerTest {
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    private ProductEvents productEvents;
    private MediaEvents mediaEvents;

    private Product testProduct;
    private UUID imageId;

    @BeforeEach
    void setUp() {
        // kafkaTemplate = new FakeKafkaTemplate();
        productEvents = new ProductEvents(kafkaTemplate);
        mediaEvents = new MediaEvents(kafkaTemplate);

        testProduct = new Product();
        testProduct.setId(UUID.randomUUID());
        testProduct.setName("Test Product");
        testProduct.setUserId("user-123");
        testProduct.setPrice(29.99);
        testProduct.setQuantity(10);
        imageId = UUID.randomUUID();
    }

    @Test
    void sendProductCreatedEventPublishesCorrectTopicAndPayload() {
        productEvents.sendCreateEvent(testProduct);
        verify(kafkaTemplate, times(1)).send(
                eq("create-product-events"),
            isNull(),
            argThat(event -> event instanceof KafkaProductCreatedEvent created
                && testProduct.getId().equals(created.id())
                && testProduct.getUserId().equals(created.userId())));
    }

    @Test
    void sendProductRemovedEventPublishesCorrectTopicAndPayload() {
        productEvents.sendRemoveEvent(testProduct);
        verify(kafkaTemplate, times(1)).send(
                eq("remove-product-events"),
            isNull(),
            argThat(event -> event instanceof KafkaProductRemovedEvent removed
                && testProduct.getId().equals(removed.id())));
    }

    @Test
    void confirmImageEventPublishesCorrectTopicAndPayload() {
        mediaEvents.confimImageEvent(imageId);
        verify(kafkaTemplate, times(1)).send(
                eq("confirm-image-events"),
            isNull(),
            argThat(event -> event instanceof KafkaConfirmImageEvent confirm
                && imageId.equals(confirm.id())));
    }

    @Test
    void deleteImageEventPublishesCorrectTopicAndPayload() {
        mediaEvents.deleteImageEvent(imageId);
        verify(kafkaTemplate, times(1)).send(
                eq("delete-image-events"),
            isNull(),
            argThat(event -> event instanceof KafkaConfirmImageEvent confirm
                && imageId.equals(confirm.id())));
    }

    @Test
    void productCreatedEventContainsCorrectData() {
        productEvents.sendCreateEvent(testProduct);
        verify(kafkaTemplate, times(1)).send(
                eq("create-product-events"),
            isNull(),
            argThat(event -> event instanceof KafkaProductCreatedEvent created
                && testProduct.getId().equals(created.id())
                && testProduct.getUserId().equals(created.userId())));
    }

    // private String eqTopic(String topic) {
    // return org.mockito.ArgumentMatchers.eq(topic);
    // }

    // lightweight fake KafkaTemplate to avoid inline Mockito on KafkaTemplate
    // static class FakeKafkaTemplate extends KafkaTemplate {
    // String lastTopic;
    // Object lastKey;
    // Object lastData;

    // @Override
    // public
    // org.springframework.util.concurrent.ListenableFuture<org.springframework.kafka.support.SendResult<String,
    // Object>> send(
    // String topic, Object key, Object data) {
    // this.lastTopic = topic;
    // this.lastKey = key;
    // this.lastData = data;
    // return null;
    // }

    // // Many methods in KafkaTemplate are not used by tests; provide no-op
    // // implementations.
    // @Override
    // public
    // org.springframework.util.concurrent.ListenableFuture<org.springframework.kafka.support.SendResult<String,
    // Object>> send(
    // String topic, Object data) {
    // return send(topic, null, data);
    // }

    // @Override
    // public
    // org.springframework.util.concurrent.ListenableFuture<org.springframework.kafka.support.SendResult<String,
    // Object>> send(
    // org.springframework.kafka.support.SendResult<String, Object> sendResult) {
    // return null;
    // }

    // // The KafkaTemplate interface has many methods; implement stubs returning
    // null
    // // or defaults.
    // @Override
    // public void flush() {
    // }

    // @Override
    // public
    // java.util.concurrent.Future<org.springframework.kafka.support.SendResult<String,
    // Object>> sendOffsetsToTransaction(
    // java.util.Map<org.apache.kafka.common.TopicPartition,
    // org.apache.kafka.clients.consumer.OffsetAndMetadata> offsets,
    // String consumerGroupId) {
    // return null;
    // }

    // @Override
    // public void sendOffsetsToTransaction(
    // java.util.Map<org.apache.kafka.common.TopicPartition,
    // org.apache.kafka.clients.consumer.OffsetAndMetadata> offsets,
    // org.springframework.kafka.support.ProducerListener<String, Object> listener)
    // {
    // }
    // // Remaining methods omitted for brevity; compile-time compatibility ensured
    // by
    // // declaring class as implementing KafkaTemplate via raw form in tests. If
    // // needed, adjust to concrete type used in project.
    // }
}