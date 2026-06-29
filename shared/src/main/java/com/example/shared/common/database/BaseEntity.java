package com.example.shared.common.database;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class BaseEntity {
    @Id
    private UUID id = UUID.randomUUID();

    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    @LastModifiedDate
    @Field("updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now(ZoneOffset.UTC);

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
