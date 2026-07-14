package com.buy01.payments.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.buy01.payments.model.PaymentOrder;

public interface PaymentOrderRepository extends MongoRepository<PaymentOrder, String> {
    Optional<PaymentOrder> findByStripeSessionId(String stripeSessionId);
    List<PaymentOrder> findAllByBuyerIdOrderByCreatedAtDesc(String buyerId);
}
