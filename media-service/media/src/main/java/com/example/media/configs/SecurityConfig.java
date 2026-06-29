package com.example.media.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.media.filters.HeaderAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    public SecurityConfig(HeaderAuthenticationFilter headerAuthenticationFilter) {
        this.headerAuthenticationFilter = headerAuthenticationFilter;
    }

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless internal API: authentication is propagated by the gateway,
                // and this service does not use browser cookie sessions.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // All GET requests to /products/** are public
                        .requestMatchers(HttpMethod.GET, "/products/**").permitAll()
                        // All GET requests to /users/** are public
                        .requestMatchers(HttpMethod.GET, "/users/**").permitAll()
                        // Non-GET requests to /products/** require SELLER or ADMIN
                        .requestMatchers("/products/", "/products/**").hasAnyRole("SELLER", "ADMIN")
                        // Non-GET requests to /users/** require BUYER, SELLER, ADMIN
                        .requestMatchers("/users/**").hasAnyRole("BUYER", "SELLER", "ADMIN")
                        // Any other request requires authentication
                        .anyRequest().authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        headerAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
