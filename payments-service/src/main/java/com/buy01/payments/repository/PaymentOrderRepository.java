package com.buy01.payments.repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.buy01.payments.model.PaymentOrder;
import com.buy01.payments.model.OrderStatus;

public interface PaymentOrderRepository extends MongoRepository<PaymentOrder, String> {
    Optional<PaymentOrder> findByStripeSessionId(String stripeSessionId);
    List<PaymentOrder> findAllByBuyerIdOrderByCreatedAtDesc(String buyerId);
    long deleteByStatusAndUpdatedAtBefore(OrderStatus status, Instant cutoff);
}
