package com.buy01.users.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminRoleUpdateReqDTOs(
        @NotBlank(message = "Role is required")
        @Pattern(regexp = "(?i)^(BUYER|SELLER|ADMIN)$", message = "Role must be BUYER, SELLER, or ADMIN")
        String role) {
}
