package com.example.products.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.products.kafka.MediaEvents;
import com.example.products.models.Product;
import com.example.products.models.User;
import com.example.shared.common.kafka.dtos.users.*;

import com.example.products.repositories.ProductRepository;

import com.example.products.repositories.UserRepository;

@Service
public class UsersService {
    private static final int DELETE_PRODUCT_BATCH_SIZE = 100;

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final MediaEvents mediaEvents;

    public UsersService(ProductRepository productRepository, UserRepository userRepository, MediaEvents mediaEvents) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.mediaEvents = mediaEvents;
    }

    public void createUser(KafkaUserCreatedEvent obj) {
        User u = new User(obj);
        this.userRepository.save(u);
    }

    public void updateUser(KafkaUserUpdatedEvent obj) {
        this.userRepository.findById(obj.userId()).ifPresent(u -> {
            u.update(obj);
            this.userRepository.save(u);
        });
    }

    public void deleteUser(KafkaUserRemovedEvent obj) {
        int page = 0;
        Page<Product> products;

        do {
            products = this.productRepository.findAllByUserId(obj.userId(),
                    PageRequest.of(page, DELETE_PRODUCT_BATCH_SIZE));
            products.forEach(p -> this.mediaEvents.deleteImageEvent(p.getImage()));
            page++;
        } while (products.hasNext());

        this.productRepository.deleteByUserId(obj.userId());
        this.userRepository.deleteById(obj.userId());
    }
}
