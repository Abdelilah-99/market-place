package com.example.products.models;


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

    @Field("user_id")
    private String userId;

    public Product(CreateProdutDto dto, String userId) {
        this.name = dto.getName();
        this.description = dto.getDescription();
        this.category = dto.getCategory();
        this.price = dto.getPrice();
        this.image = dto.getImage();
        this.userId = userId;
    }

}
