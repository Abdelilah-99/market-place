package com.example.products.repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.products.models.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, UUID> {
    Page<Product> findAllByUserId(String userId, Pageable pageable);

    Page<Product> findAllByCategoryIgnoreCase(String category, Pageable pageable);

    void deleteByUserId(String userId);
}
