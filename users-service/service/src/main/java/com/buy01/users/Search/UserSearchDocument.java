package com.buy01.users.Search;

import java.util.UUID;

import com.buy01.users.Entity.User;

public record UserSearchDocument(
        String id,
        String username,
        String email,
        String role,
        UUID avatarUrl) {

    public static UserSearchDocument fromUser(User user) {
        UUID avatar = user.avatarUrl() == null || user.avatarUrl().isBlank()
                ? null
                : UUID.fromString(user.avatarUrl());

        return new UserSearchDocument(
                user.id(),
                user.name(),
                user.email(),
                user.role(),
                avatar);
    }
}
