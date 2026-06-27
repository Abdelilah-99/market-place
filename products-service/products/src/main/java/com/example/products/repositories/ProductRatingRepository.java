package com.example.products.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.products.models.ProductRating;

@Repository
public interface ProductRatingRepository extends MongoRepository<ProductRating, UUID> {
    List<ProductRating> findAllByProductId(UUID productId);

    Optional<ProductRating> findByProductIdAndUserId(UUID productId, String userId);

    void deleteByProductId(UUID productId);
}
