package com.buy01.users.DTOs;

import java.util.UUID;

public record PublicProfileResDTO(
        String id,
        String username,
        String role,
        UUID avatarUrl) {
    public PublicProfileResDTO(String id, String username, String role, String avatarUrl) {
        this(id, username, role,
                avatarUrl != null && !avatarUrl.isBlank() ? UUID.fromString(avatarUrl) : null);
    }
}
