package com.buy01.users.Service;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.buy01.users.DTOs.LoginReqDTOs;
import com.buy01.users.DTOs.LoginResDTOs;
import com.buy01.users.DTOs.RegisterReqDTOs;
import com.buy01.users.DTOs.RegisterResDTOs;
import com.buy01.users.Entity.User;
import com.buy01.users.Exceptions.UserExistException;
import com.buy01.users.Repository.UserRepository;
import com.buy01.users.Utils.JwtUtils;
import com.example.shared.common.kafka.dtos.media.KafkaConfirmAvatarEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserCreatedEvent;
import com.example.shared.common.types.Role;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtils jwtUtils,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.kafkaTemplate = kafkaTemplate;
    }

    public RegisterResDTOs register(RegisterReqDTOs req) {
        Role role = normalizeRole(req.role());
        boolean exist = userRepository.existsByEmail(req.email());

        if (exist) {
            throw new UserExistException("Email is already registered");
        }
        String avatarUUID = null;
        if (req.avatarUrl() != null) {
            avatarUUID = req.avatarUrl().toString();
        }
        User user = new User(null, req.name(), req.email(), passwordEncoder.encode(req.password()),
                role.toString().substring(5), avatarUUID);

        user = userRepository.save(user);
        UUID avatar = (user.avatarUrl() != null && !user.avatarUrl().isBlank())
                ? UUID.fromString(user.avatarUrl())
                : null;
        if (avatar != null) {
            KafkaConfirmAvatarEvent event = new KafkaConfirmAvatarEvent(avatar);
            kafkaTemplate.send("confirm-avatar-events", null, event);
        }
        KafkaUserCreatedEvent event = new KafkaUserCreatedEvent(user.id(), user.name(),
                avatar);

        kafkaTemplate.send("create-user-events", null, event);

        return new RegisterResDTOs("user created");
    }

    public LoginResDTOs login(LoginReqDTOs req) {
        User user = userRepository.findByEmail(req.identification())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (passwordEncoder.matches(req.password(), user.password())) {
            String token = jwtUtils.generateToken(user.id(), user.role());
            return new LoginResDTOs(token, user.role(), "ok");
        }
        throw new BadCredentialsException("Invalid credentials");
    }

    private Role normalizeRole(String roleInput) {
        if (roleInput == null || roleInput.isBlank()) {
            return Role.ROLE_BUYER;
        }

        String normalized = roleInput.trim().toUpperCase();

        return switch (normalized) {
            case "BUYER" -> Role.ROLE_BUYER;
            case "SELLER" -> Role.ROLE_SELLER;
            default -> throw new IllegalArgumentException(
                    "Invalid role. Use BUYER or SELLER");
        };
    }
}
