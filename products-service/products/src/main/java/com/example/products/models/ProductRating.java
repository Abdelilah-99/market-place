package com.example.products.models;

import java.util.UUID;

import com.example.shared.common.database.BaseEntity;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "product_ratings")
@CompoundIndex(name = "product_user_rating_idx", def = "{'product_id': 1, 'user_id': 1}", unique = true)
public class ProductRating extends BaseEntity {
    @Field("product_id")
    private UUID productId;

    @Field("user_id")
    private String userId;

    private int stars;

    public ProductRating(UUID productId, String userId, int stars) {
        this.productId = productId;
        this.userId = userId;
        this.stars = stars;
    }
}
