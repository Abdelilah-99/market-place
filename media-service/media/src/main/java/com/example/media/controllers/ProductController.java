package com.example.media.controllers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import java.security.Principal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.media.models.ProductImage;
import com.example.media.repositories.ProductImageRepository;
import com.example.media.services.ProductImageService;
import com.example.media.stores.ProductimageContentStore;

import jakarta.annotation.security.PermitAll;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductImageService productImageService;
    private final ProductImageRepository repository;
    private final ProductimageContentStore contentStore;

    public ProductController(ProductImageService productImageService,
            ProductImageRepository repository,
            ProductimageContentStore contentStore) {
        this.productImageService = productImageService;
        this.repository = repository;
        this.contentStore = contentStore;
    }

    @PostMapping("/")
    public ResponseEntity<?> uploadImage(
            @RequestBody byte[] fileBytes,
            @RequestHeader("Content-Type") String mimeType, Authentication authentication) throws Exception {

        if (!this.productImageService.isImageMimeType(mimeType)) {
            return ResponseEntity
                    .badRequest()
                    .body("File must be an image");
        }

        String userId = extractUserId(authentication);

        ProductImage avatar = productImageService.uploadAvatar(
                new ByteArrayInputStream(fileBytes),
                mimeType, userId);

        System.out.println("Image uploaded =====> " + avatar.getId());

        return ResponseEntity.ok(avatar.getId().toString());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteImage(@PathVariable UUID id, Authentication authentication) throws Exception {
        String userId = extractUserId(authentication);

        ProductImage image = this.productImageService.getAvatarbyId(id);
        if (image == null) {
            return ResponseEntity.notFound().build();
        }

        if (!image.getUserId().equals(userId)) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("You are not the owner of the image");
        }

        this.productImageService.deleteImage(image);

        return ResponseEntity.noContent().build();
    }

    private String extractUserId(Authentication authentication) {
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        Object principal = authentication.getPrincipal();
        if (principal == null) return authentication.getName();
        if (principal instanceof String) return (String) principal;
        if (principal instanceof UserDetails) return ((UserDetails) principal).getUsername();
        if (principal instanceof Principal) return ((Principal) principal).getName();
        return authentication.getName();
    }

    @GetMapping("/{id}")
    @PermitAll
    public ResponseEntity<?> getImage(@PathVariable UUID id) {

        Optional<ProductImage> optionalImage = repository.findById(id);

        if (optionalImage.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ProductImage image = optionalImage.get();

        try (InputStream is = contentStore.getContent(image)) {

            byte[] bytes = is.readAllBytes();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.getMimeType()))
                    .contentLength(image.getContentLength())
                    .body(bytes);

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Error reading image");
        }
    }

}
