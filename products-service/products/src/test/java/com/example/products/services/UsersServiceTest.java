package com.example.products.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.example.products.kafka.MediaEvents;
import com.example.products.models.Product;
import com.example.products.models.User;
import com.example.products.repositories.ProductRepository;
import com.example.products.repositories.UserRepository;
import com.example.shared.common.kafka.dtos.users.KafkaUserCreatedEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserRemovedEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserUpdatedEvent;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UsersServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MediaEvents mediaEvents;

    private UsersService usersService;

    @BeforeEach
    void setUp() {
        usersService = new UsersService(productRepository, userRepository, mediaEvents);
    }

    @Test
    void createUserSavesUserEntity() {
        KafkaUserCreatedEvent event = new KafkaUserCreatedEvent("u-1", "Alice", UUID.randomUUID());

        usersService.createUser(event);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUserUpdatesAndSavesWhenUserExists() {
        User existing = new User();
        when(userRepository.findById("u-1")).thenReturn(Optional.of(existing));

        usersService.updateUser(new KafkaUserUpdatedEvent("u-1", "Bob", UUID.randomUUID(), UUID.randomUUID()));

        verify(userRepository).save(existing);
    }

    @Test
    void deleteUserDeletesProductImagesProductsAndUser() {
        String userId = "u-1";
        Product p1 = new Product();
        Product p2 = new Product();
        p1.setImage(UUID.randomUUID());
        p2.setImage(UUID.randomUUID());

        when(productRepository.findAllByUserId(org.mockito.ArgumentMatchers.eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p1, p2)));

        usersService.deleteUser(new KafkaUserRemovedEvent(userId));

        verify(mediaEvents).deleteImageEvent(p1.getImage());
        verify(mediaEvents).deleteImageEvent(p2.getImage());
        verify(productRepository).deleteByUserId(userId);
        verify(userRepository).deleteById(userId);
    }
}
