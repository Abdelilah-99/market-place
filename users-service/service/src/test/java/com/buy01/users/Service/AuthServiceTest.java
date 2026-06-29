package com.buy01.users.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.buy01.users.DTOs.LoginReqDTOs;
import com.buy01.users.DTOs.LoginResDTOs;
import com.buy01.users.DTOs.RegisterReqDTOs;
import com.buy01.users.DTOs.RegisterResDTOs;
import com.buy01.users.Entity.User;
import com.buy01.users.Exceptions.UserExistException;
import com.buy01.users.Repository.UserRepository;
import com.buy01.users.Utils.JwtUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtils, kafkaTemplate);
    }

    @Test
    void registerThrowsWhenEmailExists() {
        RegisterReqDTOs req = new RegisterReqDTOs("mail@example.com", "Alice", "secret123", "BUYER", null);
        when(userRepository.existsByEmail(req.email())).thenReturn(true);

        assertThrows(UserExistException.class, () -> authService.register(req));
    }

    @Test
    void registerSavesUserAndEmitsKafkaEvents() {
        UUID avatar = UUID.randomUUID();
        RegisterReqDTOs req = new RegisterReqDTOs("mail@example.com", "Alice", "secret123", "SELLER", avatar);

        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        when(userRepository.save(any(User.class)))
                .thenReturn(new User("u-1", "Alice", "mail@example.com", "hashed", "SELLER", avatar.toString()));

        RegisterResDTOs response = authService.register(req);

        assertEquals("user created", response.msg());
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("confirm-avatar-events"),
                org.mockito.ArgumentMatchers.isNull(), any());
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("create-user-events"),
                org.mockito.ArgumentMatchers.isNull(), any());
    }

    @Test
    void loginReturnsTokenForValidCredentials() {
        LoginReqDTOs req = new LoginReqDTOs("mail@example.com", "secret123");
        User user = new User("u-1", "Alice", "mail@example.com", "hashed", "BUYER", null);

        when(userRepository.findByEmail(req.identification())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), user.password())).thenReturn(true);
        when(jwtUtils.generateToken(user.id(), user.role())).thenReturn("jwt-token");

        LoginResDTOs result = authService.login(req);

        assertEquals("jwt-token", result.token());
        assertEquals("BUYER", result.role());
        assertEquals("ok", result.message());
    }

    @Test
    void loginThrowsForInvalidCredentials() {
        LoginReqDTOs req = new LoginReqDTOs("mail@example.com", "wrong");
        User user = new User("u-1", "Alice", "mail@example.com", "hashed", "BUYER", null);

        when(userRepository.findByEmail(req.identification())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.password(), user.password())).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }
}
