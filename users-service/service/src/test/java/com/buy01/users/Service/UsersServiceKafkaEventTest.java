package com.buy01.users.Service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.buy01.users.DTOs.ProfileUpdateReqDTOs;
import com.buy01.users.DTOs.RegisterReqDTOs;
import com.buy01.users.Entity.User;
import com.buy01.users.Repository.UserRepository;
import com.buy01.users.Utils.JwtUtils;
import com.example.shared.common.kafka.dtos.media.KafkaConfirmAvatarEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserCreatedEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserRemovedEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserUpdatedEvent;

@ExtendWith(MockitoExtension.class)
class UsersServiceKafkaEventTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AuthService authService;

    @InjectMocks
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

    authService.register(registerDto);

    verify(userRepository).save(any(User.class));
    verify(kafkaTemplate).send(
        eq("create-user-events"),
        isNull(),
        argThat(event -> event instanceof KafkaUserCreatedEvent created
            && userId.equals(created.userId())
            && userName.equals(created.username())
            && created.avatar() == null));
    }

    @Test
    void testUserUpdatedEventEmittedOnProfileUpdate() {
    UUID newAvatarId = UUID.randomUUID();
    User user = new User(userId, "Old Name", userEmail, "hashed-password", "BUYER", null);
    ProfileUpdateReqDTOs updateDto = new ProfileUpdateReqDTOs("New Name", null, newAvatarId);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class)))
        .thenReturn(new User(userId, "New Name", userEmail, "hashed-password", "BUYER", newAvatarId.toString()));

    profileService.updateCurrentProfile(updateDto);

    verify(userRepository).save(any(User.class));
    verify(kafkaTemplate).send(
        eq("confirm-avatar-events"),
        isNull(),
        argThat(event -> event instanceof KafkaConfirmAvatarEvent confirm
            && newAvatarId.equals(confirm.id())));
    verify(kafkaTemplate).send(
        eq("update-user-events"),
        isNull(),
        argThat(event -> event instanceof KafkaUserUpdatedEvent updated
            && userId.equals(updated.userId())
            && "New Name".equals(updated.username())
            && updated.oldAvatar() == null
            && newAvatarId.equals(updated.newAvatar())));
    }

    @Test
    void testUserRemovedEventEmittedOnDelete() {
    when(userRepository.findById(userId))
        .thenReturn(Optional.of(new User(userId, userName, userEmail, "hashed-password", "BUYER", null)));

    profileService.deleteCurrentUser();

    verify(userRepository).deleteById(userId);
    verify(kafkaTemplate).send(
        eq("remove-user-events"),
        isNull(),
        argThat(event -> event instanceof KafkaUserRemovedEvent removed
            && userId.equals(removed.userId())));
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

    verify(kafkaTemplate, times(1)).send(
        eq("confirm-avatar-events"),
        isNull(),
        argThat(event -> event instanceof KafkaConfirmAvatarEvent confirm
            && avatarId.equals(confirm.id())));
    verify(kafkaTemplate).send(
        eq("create-user-events"),
        isNull(),
        argThat(event -> event instanceof KafkaUserCreatedEvent created
            && userId.equals(created.userId())
            && userName.equals(created.username())
            && avatarId.equals(created.avatar())));

    verify(userRepository).save(any(User.class));
    }
}
