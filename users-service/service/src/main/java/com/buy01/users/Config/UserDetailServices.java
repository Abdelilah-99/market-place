package com.buy01.users.Config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.buy01.users.Entity.User;
import com.buy01.users.Repository.UserRepository;

@Component
public class UserDetailServices implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetailServices(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not Found 1"));
        String username = user.id();
        // String email = user.email();
        String password = user.password();
        List<SimpleGrantedAuthority> roles = new ArrayList<>();
        String role = user.role() == null ? "GUEST" : user.role();
        roles.add(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role));
        // public User(String username, @Nullable String password, Collection<? extends
        // GrantedAuthority> authorities) {
        // this(username, password, true, true, true, true, authorities);
        // }
        return new org.springframework.security.core.userdetails.User(
                username,
                password,
                true,
                true,
                true,
                true,
                roles);
    }
}
