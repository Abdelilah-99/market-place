package com.buy01.payments.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.buy01.payments.model.OrderStatus;
import com.buy01.payments.repository.PaymentOrderRepository;

import io.micrometer.observation.annotation.Observed;

@Service
public class PaymentOrderRetentionJob {
    private static final Logger log = LoggerFactory.getLogger(PaymentOrderRetentionJob.class);
    private final PaymentOrderRepository repository;
    private final long cancelledRetentionDays;
    private final long paidRetentionDays;

    public PaymentOrderRetentionJob(PaymentOrderRepository repository,
            @Value("${orders.retention.cancelled-days:30}") long cancelledRetentionDays,
            @Value("${orders.retention.paid-days:365}") long paidRetentionDays) {
        this.repository = repository;
        this.cancelledRetentionDays = positive(cancelledRetentionDays, "cancelled");
        this.paidRetentionDays = positive(paidRetentionDays, "paid");
    }

    @Scheduled(cron = "${orders.retention.cron:0 20 3 * * *}", zone = "UTC")
    @Observed(name = "marketplace.payment.orders.cleanup", contextualName = "cleanup-payment-orders")
    public void deleteExpiredOrders() {
        Instant now = Instant.now();
        long cancelled = repository.deleteByStatusAndUpdatedAtBefore(OrderStatus.CANCELLED,
                now.minus(cancelledRetentionDays, ChronoUnit.DAYS));
        long paid = repository.deleteByStatusAndUpdatedAtBefore(OrderStatus.PAID,
                now.minus(paidRetentionDays, ChronoUnit.DAYS));
        if (cancelled + paid > 0) {
            log.info("Deleted expired payment orders: cancelled={}, paid={}", cancelled, paid);
        }
    }

    private long positive(long value, String status) {
        if (value < 1) {
            throw new IllegalArgumentException(status + " order retention must be at least one day");
        }
        return value;
    }
}
