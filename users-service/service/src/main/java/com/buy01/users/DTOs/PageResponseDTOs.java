package com.buy01.users.DTOs;

import java.util.List;

public record PageResponseDTOs<T>(
        List<T> items,
        long total,
        int page,
        int size,
        int totalPages,
        boolean hasNext) {
}
