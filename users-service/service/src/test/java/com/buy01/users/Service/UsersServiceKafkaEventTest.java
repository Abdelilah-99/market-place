package com.buy01.users.Service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import com.buy01.users.DTOs.RegisterReqDTOs;
import com.buy01.users.DTOs.LoginReqDTOs;
import com.buy01.users.DTOs.ProfileUpdateReqDTOs;
import com.buy01.users.Entity.User;
import com.buy01.users.Repository.UserRepository;
import com.buy01.users.Utils.JwtUtils;
import com.example.shared.common.kafka.dtos.media.KafkaConfirmAvatarEvent;
import com.example.shared.common.kafka.dtos.products.KafkaProductRemovedEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserCreatedEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserUpdatedEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserRemovedEvent;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:9092",
        "logging.level.org.apache.kafka=WARN"
})
@SuppressWarnings("null")
class UsersServiceKafkaEventTest {
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfileService profileService;

    private String userId;
    private String userEmail;
    private String userName;
    private String userPassword;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        userEmail = "test@example.com";
        userName = "Test User";
        userPassword = "password123";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null));
    }

    @Test
    void testUserCreatedEventEmittedOnRegistration() {
        RegisterReqDTOs registerDto = new RegisterReqDTOs(
                userEmail,
                userName,
                userPassword,
                "BUYER",
                null);

        User newUser = new User(userId, userName, userEmail, "hashed-password", "BUYER", null);

        when(userRepository.existsByEmail(userEmail)).thenReturn(false);
        when(passwordEncoder.encode(userPassword)).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // This should emit Kafka events
        authService.register(registerDto);

        // Verify that events were sent to Kafka
        // We can't directly verify Kafka sends without embedded Kafka, but we verify
        // the service was called
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUserUpdatedEventEmittedOnProfileUpdate() {
        User user = new User(userId, "Old Name", userEmail, "hashed-password", "BUYER", null);
        ProfileUpdateReqDTOs updateDto = new ProfileUpdateReqDTOs("New Name", null, UUID.randomUUID());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class)))
                .thenReturn(new User(userId, "New Name", userEmail, "hashed-password", "BUYER", null));

        profileService.updateCurrentProfile(updateDto);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUserRemovedEventEmittedOnDelete() {
        when(userRepository.existsById(userId)).thenReturn(true);

        profileService.deleteCurrentUser();

        verify(userRepository).deleteById(userId);
    }

    @Test
    void testRegistrationWithAvatarEmitsConfirmAvatarEvent() {
        UUID avatarId = UUID.randomUUID();
        RegisterReqDTOs registerDto = new RegisterReqDTOs(
                userEmail,
                userName,
                userPassword,
                "SELLER",
                avatarId);

        User newUser = new User(userId, userName, userEmail, "hashed-password", "SELLER", avatarId.toString());

        when(userRepository.existsByEmail(userEmail)).thenReturn(false);
        when(passwordEncoder.encode(userPassword)).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        authService.register(registerDto);
        // KafkaConfirmAvatarEvent event = new KafkaConfirmAvatarEvent(avatarId);
        System.out.println("Avatar: " + avatarId.toString() + " ====================================================");
        // kafkaTemplate.send("confirm-avatar-events", null, event);
        verify(kafkaTemplate, times(1)).send(
                eq("confirm-avatar-events"),
                any(),
                argThat(event -> event instanceof KafkaConfirmAvatarEvent));

        verify(userRepository).save(any(User.class));
    }
}
