package com.buy01.users.Search;

import com.buy01.users.Entity.User;

public record UserSearchDocument(
        String id,
        String username,
        String email,
        String role,
        String avatarUrl) {

    public static UserSearchDocument fromUser(User user) {
        return new UserSearchDocument(
                user.id(),
                user.name(),
                user.email(),
                user.role(),
                user.avatarUrl());
    }
}
