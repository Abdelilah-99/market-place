package com.buy01.payments.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("payment_orders")
public class PaymentOrder {
    @Id
    private String id;
    @Indexed(unique = true, sparse = true)
    private String stripeSessionId;
    private String productId;
    private String productName;
    private long quantity;
    private BigDecimal unitPrice;
    private String currency;
    @Indexed
    private String buyerId;
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public PaymentOrder() { }

    public PaymentOrder(String id, String productId, String productName, long quantity,
            BigDecimal unitPrice, String currency, String buyerId) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.currency = currency;
        this.buyerId = buyerId;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public long getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getCurrency() { return currency; }
    public String getBuyerId() { return buyerId; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
