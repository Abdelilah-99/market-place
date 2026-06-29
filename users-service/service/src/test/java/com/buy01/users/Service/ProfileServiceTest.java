package com.buy01.users.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.buy01.users.DTOs.ProfileResDTOs;
import com.buy01.users.DTOs.ProfileUpdateReqDTOs;
import com.buy01.users.DTOs.RegisterResDTOs;
import com.buy01.users.Entity.User;
import com.buy01.users.Repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(userRepository, kafkaTemplate);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("u-1", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentProfileReturnsAuthenticatedUserData() {
        when(userRepository.findById("u-1"))
                .thenReturn(Optional.of(new User("u-1", "Alice", "mail@example.com", "secret", "BUYER", null)));

        ProfileResDTOs profile = profileService.getCurrentProfile();

        assertEquals("u-1", profile.id());
        assertEquals("Alice", profile.username());
    }

    @Test
    void updateCurrentProfileSavesAndPublishesEvents() {
        UUID oldAvatar = UUID.randomUUID();
        UUID newAvatar = UUID.randomUUID();
        User existing = new User("u-1", "Alice", "mail@example.com", "secret", "BUYER", oldAvatar.toString());

        when(userRepository.findById("u-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResDTOs result = profileService
                .updateCurrentProfile(new ProfileUpdateReqDTOs("Alice Updated", "mail2@example.com", newAvatar));

        assertEquals("Alice Updated", result.username());
        assertEquals("mail2@example.com", result.email());
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("confirm-avatar-events"),
                org.mockito.ArgumentMatchers.isNull(), any());
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("update-user-events"),
                org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void deleteCurrentUserDeletesAndSendsEventWhenUserExists() {
        when(userRepository.findById("u-1"))
                .thenReturn(Optional.of(new User("u-1", "Alice", "mail@example.com", "secret", "BUYER", null)));

        RegisterResDTOs result = profileService.deleteCurrentUser();

        assertEquals("user deleted successfully", result.msg());
        verify(userRepository).deleteById("u-1");
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("remove-user-events"),
                org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void deleteCurrentUserThrowsWhenMissing() {
        when(userRepository.findById("u-1")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> profileService.deleteCurrentUser());
    }
}
