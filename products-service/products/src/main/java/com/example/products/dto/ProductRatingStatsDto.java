package com.example.products.dto;

import java.util.Map;

public record ProductRatingStatsDto(
        double average,
        long count,
        Map<Integer, Long> breakdown,
        Integer myRating) {
}
