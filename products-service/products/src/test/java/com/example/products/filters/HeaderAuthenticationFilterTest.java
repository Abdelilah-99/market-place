package com.example.products.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.products.repositories.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

class HeaderAuthenticationFilterTest {

    private UserRepository userRepository;
    private HeaderAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userRepository = mock(UserRepository.class);
        filter = new HeaderAuthenticationFilter(userRepository);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void authenticatedProductRequestPassesEvenWhenLocalUserMirrorIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products/");
        request.addHeader("X-User-Id", "user-123");
        request.addHeader("X-User-Role", "BUYER");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRepository.existsById("user-123")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).existsById("user-123");
    }

    @Test
    void guestRequestWithoutHeadersStillPasses() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/products/");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain).doFilter(request, response);
    }
}