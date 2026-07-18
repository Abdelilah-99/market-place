package com.buy01.payments.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.buy01.payments.model.OrderStatus;
import com.buy01.payments.repository.PaymentOrderRepository;

class PaymentOrderRetentionJobTest {
    @Test
    void removesOnlyExpiredTerminalOrders() {
        PaymentOrderRepository repository = mock(PaymentOrderRepository.class);

        new PaymentOrderRetentionJob(repository, 30, 365).deleteExpiredOrders();

        verify(repository).deleteByStatusAndUpdatedAtBefore(eq(OrderStatus.CANCELLED), any(Instant.class));
        verify(repository).deleteByStatusAndUpdatedAtBefore(eq(OrderStatus.PAID), any(Instant.class));
    }

    @Test
    void rejectsUnsafeRetention() {
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentOrderRetentionJob(mock(PaymentOrderRepository.class), 0, 365));
    }
}
