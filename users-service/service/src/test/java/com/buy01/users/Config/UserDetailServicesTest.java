package com.buy01.users.Config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.buy01.users.Entity.User;
import com.buy01.users.Repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailServicesTest {

    @Mock
    private UserRepository userRepository;

    private UserDetailServices userDetailServices;

    @BeforeEach
    void setUp() {
        userDetailServices = new UserDetailServices(userRepository);
    }

    @Test
    void loadUserByUsernameAddsRolePrefixWhenMissing() {
        when(userRepository.findById("u-1"))
                .thenReturn(Optional.of(new User("u-1", "Alice", "mail@example.com", "secret", "ADMIN", null)));

        UserDetails result = userDetailServices.loadUserByUsername("u-1");

        assertEquals("u-1", result.getUsername());
        assertEquals("secret", result.getPassword());
        assertTrue(result.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    @Test
    void loadUserByUsernameKeepsExistingRolePrefix() {
        when(userRepository.findById("u-1"))
                .thenReturn(Optional.of(new User("u-1", "Alice", "mail@example.com", "secret", "ROLE_SELLER", null)));

        UserDetails result = userDetailServices.loadUserByUsername("u-1");

        assertTrue(result.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_SELLER".equals(authority.getAuthority())));
    }

    @Test
    void loadUserByUsernameUsesUserDefaultWhenRoleIsNull() {
        when(userRepository.findById("u-1"))
                .thenReturn(Optional.of(new User("u-1", "Alice", "mail@example.com", "secret", null, null)));

        UserDetails result = userDetailServices.loadUserByUsername("u-1");

        assertTrue(result.getAuthorities().stream().anyMatch(authority -> "ROLE_CLIENT".equals(authority.getAuthority())));
    }

    @Test
    void loadUserByUsernameThrowsWhenMissing() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userDetailServices.loadUserByUsername("missing"));
    }
}
