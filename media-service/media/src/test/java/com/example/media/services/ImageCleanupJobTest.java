package com.example.media.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.example.media.models.ProductImage;
import com.example.media.models.UserAvatar;
import com.example.media.repositories.AvatarRepository;
import com.example.media.repositories.ProductImageRepository;
import com.example.media.stores.ProductimageContentStore;
import com.example.media.stores.UserAvatarContentStore;
import com.example.shared.common.types.ImageStatus;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ImageCleanupJobTest {

    @Mock
    private ProductImageRepository imageRepository;

    @Mock
    private ProductimageContentStore imageContentStore;

    @Mock
    private AvatarRepository avatarRepository;

    @Mock
    private UserAvatarContentStore avatarContentStore;

    private ImageCleanupJob imageCleanupJob;

    @BeforeEach
    void setUp() {
        imageCleanupJob = new ImageCleanupJob(imageRepository, imageContentStore, avatarRepository, avatarContentStore);
    }

    @Test
    void deleteExpiredTemporaryImagesDeletesBothImageAndAvatarResources() {
        ProductImage image = new ProductImage();
        UserAvatar avatar = new UserAvatar();

        when(imageRepository.findByStatusAndCreatedAtBefore(org.mockito.ArgumentMatchers.eq(ImageStatus.TEMPORARY),
            any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(image));
        when(avatarRepository.findByStatusAndCreatedAtBefore(org.mockito.ArgumentMatchers.eq(ImageStatus.TEMPORARY),
            any(LocalDateTime.class), any(Pageable.class))).thenReturn(List.of(avatar));

        imageCleanupJob.deleteExpiredTemporaryImages();

        verify(imageContentStore).unsetContent(image);
        verify(imageRepository).delete(image);
        verify(avatarContentStore).unsetContent(avatar);
        verify(avatarRepository).delete(avatar);
    }
}
