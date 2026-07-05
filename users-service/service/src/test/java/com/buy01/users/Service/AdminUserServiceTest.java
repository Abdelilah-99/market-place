package com.buy01.users.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import com.buy01.users.DTOs.AdminRoleUpdateReqDTOs;
import com.buy01.users.DTOs.PageResponseDTOs;
import com.buy01.users.DTOs.ProfileResDTOs;
import com.buy01.users.DTOs.RegisterResDTOs;
import com.buy01.users.Entity.User;
import com.buy01.users.Repository.UserRepository;
import com.buy01.users.Search.UserSearchService;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private UserSearchService userSearchService;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository, kafkaTemplate, userSearchService);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("admin-1", "password"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUsersClampsPageAndSizeAndMapsProfiles() {
        UUID avatar = UUID.randomUUID();
        User user = new User("u-1", "Alice", "mail@example.com", "secret", "BUYER", avatar.toString());

        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        PageResponseDTOs<ProfileResDTOs> result = adminUserService.getUsers(-5, 500);

        assertEquals(1, result.items().size());
        assertEquals("Alice", result.items().get(0).username());
        assertEquals(avatar, result.items().get(0).avatarUrl());
        assertEquals(0, result.page());
        assertEquals(1, result.totalPages());
        assertEquals(false, result.hasNext());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(pageableCaptor.capture());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
    }

    @Test
    void updateRoleNormalizesAndSavesUser() {
        User existing = new User("u-1", "Alice", "mail@example.com", "secret", "BUYER", null);

        when(userRepository.findById("u-1")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProfileResDTOs result = adminUserService.updateRole("u-1", new AdminRoleUpdateReqDTOs(" seller "));

        assertEquals("SELLER", result.role());
        assertNull(result.avatarUrl());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("SELLER", userCaptor.getValue().role());
        verify(userSearchService).indexUser(any(User.class));
    }

    @Test
    void updateRoleThrowsWhenUserIsMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> adminUserService.updateRole("missing", new AdminRoleUpdateReqDTOs("ADMIN")));
    }

    @Test
    void deleteUserRejectsDeletingCurrentAdmin() {
        assertThrows(ResponseStatusException.class, () -> adminUserService.deleteUser("admin-1"));
    }

    @Test
    void deleteUserDeletesAndPublishesEvent() {
        User user = new User("u-1", "Alice", "mail@example.com", "secret", "BUYER", null);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));

        RegisterResDTOs result = adminUserService.deleteUser("u-1");

        assertEquals("user deleted successfully", result.msg());
        verify(userRepository).deleteById("u-1");
        verify(userSearchService).deleteUser("u-1");
        verify(kafkaTemplate).send(org.mockito.ArgumentMatchers.eq("remove-user-events"),
                org.mockito.ArgumentMatchers.isNull(), any());
    }
}
