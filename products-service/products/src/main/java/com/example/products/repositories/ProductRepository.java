package com.example.products.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.products.models.Product;

@Repository
public interface ProductRepository extends MongoRepository<Product, UUID> {
    List<Product> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Page<Product> findAllByUserId(String userId, Pageable pageable);

    List<Product> findAllByCategoryIgnoreCaseOrderByCreatedAtDesc(String category);

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findByUserId(String userId);

    void deleteByUserId(String userId);
}
