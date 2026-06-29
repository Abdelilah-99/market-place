package com.example.products.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.products.kafka.MediaEvents;
import com.example.products.models.Product;
import com.example.products.models.User;
import com.example.shared.common.kafka.dtos.users.*;

import com.example.products.repositories.ProductRepository;

import com.example.products.repositories.UserRepository;

@Service
public class UsersService {

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
        List<Product> products = this.productRepository.findByUserId(obj.userId());
        for (Product p : products) {
            this.mediaEvents.deleteImageEvent(p.getImage());
        }

        this.productRepository.deleteByUserId(obj.userId());
        this.userRepository.deleteById(obj.userId());
    }
}
