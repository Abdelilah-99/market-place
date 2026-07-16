package com.example.products.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.products.services.ProductService;
import io.micrometer.observation.annotation.Observed;

@Component
public class ItemSoldEventListener {
    private static final Logger log = LoggerFactory.getLogger(ItemSoldEventListener.class);
    private final ProductService productService;

    public ItemSoldEventListener(ProductService productService) {
        this.productService = productService;
    }

    @Observed(name = "marketplace.inventory.sale.consume", contextualName = "consume-item-sold-event")
    @KafkaListener(topics = "item-sold-events", groupId = "products-inventory",
            containerFactory = "itemSoldKafkaListenerContainerFactory")
    public void onItemSold(ItemSoldEvent event) {
        if (!productService.confirmReservation(event.reservationId(), event.productId(), event.quantity())) {
            log.warn("Sale event {} could not confirm reservation {}", event.eventId(), event.reservationId());
        }
    }
}
