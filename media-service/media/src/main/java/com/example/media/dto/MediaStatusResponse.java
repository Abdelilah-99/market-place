package com.example.media.dto;

import java.util.UUID;

import com.example.shared.common.types.ImageStatus;

public record MediaStatusResponse(
        UUID id,
        boolean metadataExists,
        boolean contentExists,
        boolean linked,
        ImageStatus status,
        Long contentLength,
        String mimeType,
        String message) {

    public static MediaStatusResponse missing(UUID id) {
        return new MediaStatusResponse(id, false, false, false, null, null, null, "Media metadata was not found");
    }
}
