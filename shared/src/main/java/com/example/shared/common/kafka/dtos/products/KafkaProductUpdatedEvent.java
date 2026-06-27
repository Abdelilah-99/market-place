package com.example.shared.common.kafka.dtos.products;

import java.util.UUID;

public record KafkaProductUpdatedEvent(UUID id, String userId) {
}
