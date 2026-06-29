package com.example.media.models;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.example.shared.common.types.ImageStatus;

import lombok.Data;

@Data
@Document(collection = "products_images")
public class ProductImage {

    @Id
    private UUID id = UUID.randomUUID();

    @Field("user_id")
    private String userId;

    @Field("product_id")
    private UUID productId;
    
    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    private ImageStatus status = ImageStatus.TEMPORARY;

    @ContentId
    private UUID contentId;

    @ContentLength
    private Long contentLength;

    @MimeType
    private String mimeType;

    private String contentPath;
}
