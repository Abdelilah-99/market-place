package com.example.products.config;

import com.example.products.filters.HeaderAuthenticationFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;

    public SecurityConfig(HeaderAuthenticationFilter headerAuthenticationFilter) {
        this.headerAuthenticationFilter = headerAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public product listing (GET /api/products)
                        .requestMatchers(HttpMethod.GET, "/api/products/")
                        .hasAnyRole("GUEST", "BUYER", "SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/products/search")
                        .hasAnyRole("GUEST", "BUYER", "SELLER", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/admin/products/reindex-search")
                        .hasRole("ADMIN")

                        // Actuator only for admin
                        .requestMatchers("/actuator/**").hasRole("ADMIN")

                        // Everything else requires authenticated user
                        .anyRequest().hasAnyRole("SELLER", "ADMIN"))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(
                        headerAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
