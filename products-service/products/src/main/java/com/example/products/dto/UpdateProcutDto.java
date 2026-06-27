package com.example.products.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProcutDto {
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @Size(min = 6, max = 255, message = "Description must be between 6 and 255 characters")
    private String description;

    @Size(max = 120, message = "Category must be less than 120 characters")
    private String category;

    @Pattern(regexp = "(?i)^(new|used)$", message = "Condition must be new or used")
    private String condition;

    @Positive(message = "Price must be greater than 0")
    private Double price;

    private UUID image;

    private List<UUID> images;
}
