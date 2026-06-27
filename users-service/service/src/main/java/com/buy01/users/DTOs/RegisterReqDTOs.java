package com.buy01.users.DTOs;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterReqDTOs(
		@NotBlank(message = "Email is required") @Email(message = "Email is invalid") String email,
		@NotBlank(message = "Name is required") @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters") String name,
		@NotBlank(message = "Password is required") @Size(min = 6, max = 30, message = "Password must be at least 6 characters") String password,
		@Pattern(regexp = "(?i)^(BUYER|SELLER)$", message = "Invalid role. Use BUYER or SELLER") String role,
		UUID avatarUrl) {
}
