package com.example.products.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateProdutDto {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(min = 6, max = 255, message = "Description must be between 6 and 255 characters")
    private String description;

    @Size(max = 120, message = "Category must be less than 120 characters")
    private String category;

    @NotBlank(message = "Condition is required")
    @Pattern(regexp = "(?i)^(new|used)$", message = "Condition must be new or used")
    private String condition;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Long quantity;


    private UUID image;

    private List<UUID> images;
}
