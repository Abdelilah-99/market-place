package com.buy01.payments.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.buy01.payments.model.OrderStatus;
import com.buy01.payments.model.PaymentOrder;
import com.buy01.payments.repository.PaymentOrderRepository;

class OrderServiceTest {
    private final PaymentOrderRepository repository = mock(PaymentOrderRepository.class);
    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final ProductClient productClient = mock(ProductClient.class);
    private final SaleEventPublisher publisher = mock(SaleEventPublisher.class);
    private final OrderService service = new OrderService(repository, mongoTemplate, productClient, publisher);

    @Test
    void completedCheckoutPublishesReservationEvent() {
        PaymentOrder order = new PaymentOrder("order-1", "product-1", "Product", 2,
                new BigDecimal("12.50"), "usd", "buyer-1");
        order.setStripeSessionId("cs_1");
        when(repository.findByStripeSessionId("cs_1")).thenReturn(Optional.of(order));

        service.completeStripeSession("cs_1");

        verify(publisher).publish("order-1", "cs_1", "product-1", 2, "buyer-1");
    }

    @Test
    void paidCheckoutIsIdempotent() {
        PaymentOrder order = new PaymentOrder("order-1", "product-1", "Product", 1,
                BigDecimal.TEN, "usd", "buyer-1");
        order.setStripeSessionId("cs_1");
        order.setStatus(OrderStatus.PAID);
        when(repository.findByStripeSessionId("cs_1")).thenReturn(Optional.of(order));

        service.completeStripeSession("cs_1");

        assertEquals(OrderStatus.PAID, order.getStatus());
    }
}
