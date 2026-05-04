package com.example.products.filters;

import java.io.IOException;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.products.repositories.UserRepository;
import com.example.shared.common.types.Role;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Order(1) 
@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public HeaderAuthenticationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String roleHeader = "ROLE_" + request.getHeader("X-User-Role");

        System.out.println("===> " + userId);
        System.out.println("===> " + roleHeader);

        if (roleHeader == null || roleHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        Role role = Role.fromString(roleHeader);
        System.out.println("====> " + role);
        if (!role.isGuest()) {
            if (userId == null || userId.isBlank() || !userRepository.existsById(userId)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            if (!isUserExist(userId)) {
                System.out.println("=============>  User Not Exist");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            var authorities = List.of(new SimpleGrantedAuthority(roleHeader));

            var authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isUserExist(String userId) {
        if (userId == null || userId.isEmpty() || userId.isBlank()) {
            return false;
        }
        return this.userRepository.existsById(userId);
    }
}
