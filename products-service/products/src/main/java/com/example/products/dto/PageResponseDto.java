package com.example.products.dto;

import java.util.List;

public record PageResponseDto<T>(
        List<T> items,
        long total,
        int page,
        int size,
        int totalPages,
        boolean hasNext) {
}
