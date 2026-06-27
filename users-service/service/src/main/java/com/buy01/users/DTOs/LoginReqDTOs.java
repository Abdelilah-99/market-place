package com.buy01.users.DTOs;

import jakarta.validation.constraints.NotBlank;

public record LoginReqDTOs(
        @NotBlank(message = "Email is required") String identification,
        @NotBlank(message = "Password is required") String password) {
}
