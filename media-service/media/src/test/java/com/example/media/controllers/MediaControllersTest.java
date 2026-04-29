package com.example.media.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
// import org.springframework.boot.test.mock.MockitoBean;
// import org.springframework.boot.test.mock.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.media.models.ProductImage;
import com.example.media.models.UserAvatar;
import com.example.media.repositories.ProductImageRepository;
import com.example.media.services.ProductImageService;
import com.example.media.services.AvatarService;
import com.example.media.stores.ProductimageContentStore;
import com.example.media.stores.UserAvatarContentStore;
import com.example.media.repositories.UserRepository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import com.example.media.stores;
@WebMvcTest({ ProductController.class, UserController.class })
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class MediaControllersTest {
        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserRepository userRepository;

        @MockitoBean
        private ProductImageService productImageService;

        @MockitoBean
        private AvatarService avatarService;

        @MockitoBean
        private ProductImageRepository productImageRepository;

        @MockitoBean(name = "productimageContentStore")
        private ProductimageContentStore productContentStore;

        @MockitoBean(name = "userAvatarContentStore")
        private UserAvatarContentStore userContentStore;

        private UUID imageId;
        private UUID avatarId;
        private String userId;
        private byte[] testFileBytes;
        private String imageMimeType;

        @BeforeEach
        void setUp() {
                imageId = UUID.randomUUID();
                avatarId = UUID.randomUUID();
                userId = "user-123";
                testFileBytes = "fake-image-data".getBytes();
                imageMimeType = "image/png";
        }

        // ===== ProductController Tests =====

        @Test
        @WithMockUser(username = "user-123")
        void testUploadProductImageSuccess() throws Exception {
                ProductImage productImage = new ProductImage();
                productImage.setId(imageId);
                productImage.setUserId(userId);
                productImage.setMimeType(imageMimeType);

                when(productImageService.isImageMimeType(imageMimeType)).thenReturn(true);
                when(productImageService.uploadAvatar(any(ByteArrayInputStream.class), anyString(), anyString()))
                                .thenReturn(productImage);

                mockMvc.perform(post("/products/")
                                .header("Content-Type", imageMimeType)
                                .content(testFileBytes))
                                .andExpect(status().isOk())
                                .andExpect(content().string(imageId.toString()));

                verify(productImageService).isImageMimeType(imageMimeType);
                verify(productImageService).uploadAvatar(any(ByteArrayInputStream.class), anyString(), anyString());
        }

        @Test
        @WithMockUser(username = "user-123")
        void testUploadProductImageInvalidMimeType() throws Exception {
                when(productImageService.isImageMimeType("application/json")).thenReturn(false);

                mockMvc.perform(post("/products/")
                                .header("Content-Type", "application/json")
                                .content(testFileBytes))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("File must be an image"));
        }

        @Test
        @WithMockUser(username = "user-123")
        void testGetProductImageSuccess() throws Exception {
                ProductImage productImage = new ProductImage();
                productImage.setId(imageId);
                productImage.setMimeType(imageMimeType);
                productImage.setContentLength((long) testFileBytes.length);

                when(productImageRepository.findById(imageId))
                                .thenReturn(Optional.of(productImage));
                when(productContentStore.getContent(productImage))
                                .thenReturn(new ByteArrayInputStream(testFileBytes));

                mockMvc.perform(get("/products/" + imageId))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(imageMimeType))
                                .andExpect(content().bytes(testFileBytes));
        }

        @Test
        void testGetProductImageNotFound() throws Exception {
                when(productImageRepository.findById(imageId))
                                .thenReturn(Optional.empty());

                mockMvc.perform(get("/products/" + imageId))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "user-123")
        void testDeleteProductImageSuccess() throws Exception {
                ProductImage productImage = new ProductImage();
                productImage.setId(imageId);
                productImage.setUserId(userId);

                when(productImageService.getAvatarbyId(imageId))
                                .thenReturn(productImage);
                doNothing().when(productImageService).deleteImage(productImage);

                mockMvc.perform(delete("/products/" + imageId))
                                .andExpect(status().isNoContent());

                verify(productImageService).deleteImage(productImage);
        }

        @Test
        @WithMockUser(username = "user-123")
        void testDeleteProductImageNotFound() throws Exception {
                when(productImageService.getAvatarbyId(imageId))
                                .thenReturn(null);

                mockMvc.perform(delete("/products/" + imageId))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "different-user")
        void testDeleteProductImageForbidden() throws Exception {
                ProductImage productImage = new ProductImage();
                productImage.setId(imageId);
                productImage.setUserId("owner-user-123");

                when(productImageService.getAvatarbyId(imageId))
                                .thenReturn(productImage);

                mockMvc.perform(delete("/products/" + imageId))
                                .andExpect(status().isForbidden())
                                .andExpect(content().string("You are not the owner of the image"));
        }

        // ===== UserController Tests =====

        @Test
        @WithMockUser(username = "user-123")
        void testUploadUserAvatarSuccess() throws Exception {
                UserAvatar userAvatar = new UserAvatar();
                userAvatar.setId(avatarId);
                userAvatar.setUserId(userId);
                userAvatar.setMimeType(imageMimeType);

                when(avatarService.isImageMimeType(imageMimeType)).thenReturn(true);
                when(avatarService.uploadAvatar(any(ByteArrayInputStream.class), anyString(), anyString()))
                                .thenReturn(userAvatar);

                mockMvc.perform(post("/users/")
                                .header("Content-Type", imageMimeType)
                                .content(testFileBytes))
                                .andExpect(status().isOk())
                                .andExpect(content().string(avatarId.toString()));

                verify(avatarService).isImageMimeType(imageMimeType);
                verify(avatarService).uploadAvatar(any(ByteArrayInputStream.class), anyString(), anyString());
        }

        @Test
        @WithMockUser(username = "user-123")
        void testUploadUserAvatarInvalidMimeType() throws Exception {
                when(avatarService.isImageMimeType("text/plain")).thenReturn(false);

                mockMvc.perform(post("/users/")
                                .header("Content-Type", "text/plain")
                                .content(testFileBytes))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("File must be an image"));
        }

        @Test
        void testGetUserAvatarSuccess() throws Exception {
                UserAvatar userAvatar = new UserAvatar();
                userAvatar.setId(avatarId);
                userAvatar.setMimeType(imageMimeType);
                userAvatar.setContentLength((long) testFileBytes.length);

                when(avatarService.getAvatarbyId(avatarId))
                                .thenReturn(userAvatar);
                when(userContentStore.getContent(userAvatar))
                                .thenReturn(new ByteArrayInputStream(testFileBytes));

                mockMvc.perform(get("/users/" + avatarId))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(imageMimeType))
                                .andExpect(content().bytes(testFileBytes));
        }

        @Test
        void testGetUserAvatarNotFound() throws Exception {
                when(avatarService.getAvatarbyId(avatarId))
                                .thenReturn(null);

                mockMvc.perform(get("/users/" + avatarId))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "user-123")
        void testDeleteUserAvatarSuccess() throws Exception {
                UserAvatar userAvatar = new UserAvatar();
                userAvatar.setId(avatarId);
                userAvatar.setUserId(userId);

                when(avatarService.getAvatarbyId(avatarId))
                                .thenReturn(userAvatar);
                doNothing().when(avatarService).deleteAvatar(userAvatar);

                mockMvc.perform(delete("/users/" + avatarId))
                                .andExpect(status().isNoContent());

                verify(avatarService).deleteAvatar(userAvatar);
        }

        @Test
        @WithMockUser(username = "user-123")
        void testDeleteUserAvatarNotFound() throws Exception {
                when(avatarService.getAvatarbyId(avatarId))
                                .thenReturn(null);

                mockMvc.perform(delete("/users/" + avatarId))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "different-user")
        void testDeleteUserAvatarForbidden() throws Exception {
                UserAvatar userAvatar = new UserAvatar();
                userAvatar.setId(avatarId);
                userAvatar.setUserId("owner-user-123");

                when(avatarService.getAvatarbyId(avatarId))
                                .thenReturn(userAvatar);

                mockMvc.perform(delete("/users/" + avatarId))
                                .andExpect(status().isForbidden())
                                .andExpect(content().string("You are not the owner of the image"));
        }
}
