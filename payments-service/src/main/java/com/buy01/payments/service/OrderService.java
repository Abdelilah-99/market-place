package com.buy01.payments.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.buy01.payments.dto.ProductSnapshot;
import com.buy01.payments.model.OrderStatus;
import com.buy01.payments.model.PaymentOrder;
import com.buy01.payments.repository.PaymentOrderRepository;
import io.micrometer.observation.annotation.Observed;

@Service
public class OrderService {
    private final PaymentOrderRepository repository;
    private final MongoTemplate mongoTemplate;
    private final ProductClient productClient;
    private final SaleEventPublisher publisher;

    public OrderService(PaymentOrderRepository repository, MongoTemplate mongoTemplate,
            ProductClient productClient, SaleEventPublisher publisher) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
        this.productClient = productClient;
        this.publisher = publisher;
    }

    public String newOrderId() {
        return UUID.randomUUID().toString();
    }

    @Observed(name = "marketplace.payment.order.create", contextualName = "create-payment-order")
    public PaymentOrder create(String orderId, ProductSnapshot product, long quantity, String buyerId) {
        return repository.save(new PaymentOrder(orderId, product.id(),
                product.name(), quantity, product.price(), "usd", buyerId));
    }

    public PaymentOrder attachStripeSession(String orderId, String sessionId) {
        PaymentOrder order = repository.findById(orderId).orElseThrow();
        order.setStripeSessionId(sessionId);
        order.setUpdatedAt(Instant.now());
        return repository.save(order);
    }

    public void cancel(String orderId) {
        transition(orderId, OrderStatus.PENDING, OrderStatus.CANCELLED);
    }

    @Observed(name = "marketplace.payment.webhook.completed", contextualName = "complete-stripe-session")
    public void completeStripeSession(String sessionId) {
        PaymentOrder order = repository.findByStripeSessionId(sessionId).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.PENDING) {
            publisher.publish(order.getId(), sessionId, order.getProductId(), order.getQuantity(), order.getBuyerId());
            transitionBySession(sessionId, OrderStatus.PENDING, OrderStatus.PAID);
        }
    }

    @Observed(name = "marketplace.payment.webhook.expired", contextualName = "expire-stripe-session")
    public void expireStripeSession(String sessionId) {
        PaymentOrder order = repository.findByStripeSessionId(sessionId).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.PENDING) {
            productClient.releaseReservation(order.getId(), order.getProductId(), order.getQuantity());
            transitionBySession(sessionId, OrderStatus.PENDING, OrderStatus.CANCELLED);
        }
    }

    public List<PaymentOrder> buyerOrders(String buyerId) {
        return repository.findAllByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    private PaymentOrder transition(String orderId, OrderStatus from, OrderStatus to) {
        return mongoTemplate.findAndModify(Query.query(Criteria.where("_id").is(orderId).and("status").is(from)),
                new Update().set("status", to).set("updatedAt", Instant.now()),
                FindAndModifyOptions.options().returnNew(true), PaymentOrder.class);
    }

    private PaymentOrder transitionBySession(String sessionId, OrderStatus from, OrderStatus to) {
        return mongoTemplate.findAndModify(Query.query(
                        Criteria.where("stripeSessionId").is(sessionId).and("status").is(from)),
                new Update().set("status", to).set("updatedAt", Instant.now()),
                FindAndModifyOptions.options().returnNew(true), PaymentOrder.class);
    }
}
