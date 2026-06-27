package com.example.products.models;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.shared.common.database.BaseEntity;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.example.products.dto.CreateProdutDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product extends BaseEntity {

    @Indexed(unique = true)
    private String name;

    private String description;

    private String category;

    private double price;

    private long quantity;

    private UUID image;

    private List<UUID> images = new ArrayList<>();

    @Field("user_id")
    private String userId;

    public Product(CreateProdutDto dto, String userId) {
        this.name = dto.getName();
        this.description = dto.getDescription();
        this.category = dto.getCategory();
        this.price = dto.getPrice();
        this.image = dto.getImage();
        this.images = normalizeImages(dto.getImage(), dto.getImages());
        this.userId = userId;
    }

    public List<UUID> getImages() {
        if ((images == null || images.isEmpty()) && image != null) {
            return List.of(image);
        }
        return images == null ? List.of() : images;
    }

    public void setImages(List<UUID> images) {
        this.images = normalizeImages(this.image, images);
    }

    private List<UUID> normalizeImages(UUID primaryImage, List<UUID> imageList) {
        List<UUID> normalized = new ArrayList<>();
        if (imageList != null) {
            for (UUID imageId : imageList) {
                if (imageId != null && !normalized.contains(imageId)) {
                    normalized.add(imageId);
                }
            }
        }
        if (normalized.isEmpty() && primaryImage != null) {
            normalized.add(primaryImage);
        }
        return normalized;
    }
}
