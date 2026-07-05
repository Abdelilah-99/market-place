package com.buy01.users.Service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.buy01.users.DTOs.AdminRoleUpdateReqDTOs;
import com.buy01.users.DTOs.PageResponseDTOs;
import com.buy01.users.DTOs.ProfileResDTOs;
import com.buy01.users.DTOs.RegisterResDTOs;
import com.buy01.users.Entity.User;
import com.buy01.users.Repository.UserRepository;
import com.buy01.users.Search.UserSearchService;
import com.example.shared.common.kafka.dtos.users.KafkaUserRemovedEvent;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserSearchService userSearchService;

    public AdminUserService(
            UserRepository userRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            UserSearchService userSearchService) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.userSearchService = userSearchService;
    }

    public PageResponseDTOs<ProfileResDTOs> getUsers(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<User> users = userRepository.findAll(PageRequest.of(safePage, safeSize, Sort.by("name").ascending()));
        return new PageResponseDTOs<>(
                users.getContent().stream().map(this::toProfile).toList(),
                users.getTotalElements(),
                users.getNumber(),
                users.getSize(),
                users.getTotalPages(),
                users.hasNext());
    }

    public ProfileResDTOs updateRole(String id, AdminRoleUpdateReqDTOs req) {
        User user = getUser(id);
        String normalizedRole = req.role().trim().toUpperCase();
        User updated = new User(user.id(), user.name(), user.email(), user.password(), normalizedRole, user.avatarUrl());
        User saved = userRepository.save(updated);
        userSearchService.indexUser(saved);
        return toProfile(saved);
    }

    public RegisterResDTOs deleteUser(String id) {
        String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        if (id.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin cannot delete their own account");
        }
        User user = getUser(id);
        userRepository.deleteById(user.id());
        userSearchService.deleteUser(user.id());
        kafkaTemplate.send("remove-user-events", null, new KafkaUserRemovedEvent(user.id()));
        return new RegisterResDTOs("user deleted successfully");
    }

    public long reindexUserSearch() {
        return userSearchService.reindexAllUsers();
    }

    private User getUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private ProfileResDTOs toProfile(User user) {
        UUID avatar = user.avatarUrl() == null || user.avatarUrl().isBlank() ? null : UUID.fromString(user.avatarUrl());
        return new ProfileResDTOs(user.id(), user.name(), user.email(), user.role(), avatar);
    }
}
