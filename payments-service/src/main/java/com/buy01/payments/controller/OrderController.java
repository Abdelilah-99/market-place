package com.buy01.payments.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.buy01.payments.model.PaymentOrder;
import com.buy01.payments.service.OrderService;

@RestController
@RequestMapping("/api/payments/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<PaymentOrder>> myOrders(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(orderService.buyerOrders(userId));
    }
}
