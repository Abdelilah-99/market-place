package com.buy01.users.Service;

import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.buy01.users.DTOs.PageResponseDTOs;
import com.buy01.users.DTOs.ProfileResDTOs;
import com.buy01.users.DTOs.ProfileUpdateReqDTOs;
import com.buy01.users.DTOs.RegisterResDTOs;
import com.buy01.users.Entity.User;
import com.buy01.users.Repository.UserRepository;
import com.buy01.users.Search.UserSearchService;
import com.example.shared.common.kafka.dtos.media.KafkaConfirmAvatarEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserRemovedEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserUpdatedEvent;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserSearchService userSearchService;

    public ProfileService(
            UserRepository userRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            UserSearchService userSearchService) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.userSearchService = userSearchService;
    }

    public ProfileResDTOs getCurrentProfile() {
        User user = getAuthenticatedUser();
        return toProfile(user);
    }

    public PageResponseDTOs<ProfileResDTOs> searchUsers(String query, int page, int size) {
        User currentUser = getAuthenticatedUser();
        return userSearchService.search(query, currentUser.id(), page, size);
    }

    public ProfileResDTOs updateCurrentProfile(ProfileUpdateReqDTOs req) {
        User user = getAuthenticatedUser();

        String updatedName = req.name() == null || req.name().isBlank() ? user.name() : req.name();
        String updatedEmail = req.email() == null || req.email().isBlank() ? user.email() : req.email();
        UUID oldAvatarUrl = (user.avatarUrl() != null && !user.avatarUrl().isBlank())
                ? UUID.fromString(user.avatarUrl())
                : null;
        UUID updatedAvatarUrl = req.uuid();
        String AvatarUrlSTr = updatedAvatarUrl != null ? updatedAvatarUrl.toString() : null;

        User updated = new User(
                user.id(),
                updatedName,
                updatedEmail,
                user.password(),
                user.role(),
                AvatarUrlSTr);

        User newUser = userRepository.save(updated);
        userSearchService.indexUser(newUser);
        if (updatedAvatarUrl != null) {
            KafkaConfirmAvatarEvent event = new KafkaConfirmAvatarEvent(updatedAvatarUrl);
            kafkaTemplate.send("confirm-avatar-events", null, event);
        }
        if (oldAvatarUrl != null && !oldAvatarUrl.equals(updatedAvatarUrl)) {
            kafkaTemplate.send("delete-avatar-events", null, new KafkaConfirmAvatarEvent(oldAvatarUrl));
        }
        KafkaUserUpdatedEvent event = new KafkaUserUpdatedEvent(newUser.id(), newUser.name(),
                oldAvatarUrl,
                updatedAvatarUrl);
        kafkaTemplate.send("update-user-events", null, event);
        return toProfile(newUser);
    }

    private User getAuthenticatedUser() {
        String authName = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findById(authName)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid user session"));
    }

    public RegisterResDTOs deleteCurrentUser() {
        User user = getAuthenticatedUser();
        userRepository.deleteById(user.id());
        userSearchService.deleteUser(user.id());
        KafkaUserRemovedEvent event = new KafkaUserRemovedEvent(user.id());
        kafkaTemplate.send("remove-user-events", null, event);
        return new RegisterResDTOs("user deleted successfully");
    }

    private ProfileResDTOs toProfile(User user) {
        return new ProfileResDTOs(user.id(), user.name(), user.email(), user.role(), user.avatarUrl());
    }
}
