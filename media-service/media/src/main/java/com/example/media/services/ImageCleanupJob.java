package com.example.media.services;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.media.models.ProductImage;
import com.example.media.models.UserAvatar;
import com.example.media.repositories.AvatarRepository;
import com.example.media.repositories.ProductImageRepository;
import com.example.media.stores.ProductimageContentStore;
import com.example.media.stores.UserAvatarContentStore;
import com.example.shared.common.types.ImageStatus;

@Service
public class ImageCleanupJob {
    private static final int CLEANUP_BATCH_SIZE = 100;

    private final ProductImageRepository imageRepository;
    private final ProductimageContentStore imageContentStore;

    private final AvatarRepository avatarRepository;
    private final UserAvatarContentStore avatarContentStore;

    public ImageCleanupJob(
            ProductImageRepository imageRepository,
            ProductimageContentStore imageContentStore,
            AvatarRepository avatarRepository,
            UserAvatarContentStore avatarContentStore) {

        this.imageRepository = imageRepository;
        this.imageContentStore = imageContentStore;
        this.avatarRepository = avatarRepository;
        this.avatarContentStore = avatarContentStore;
    }

    @Scheduled(fixedRate = 1 * 60 * 1000) // every 1 minutes
    @Transactional
    public void deleteExpiredTemporaryImages() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        PageRequest cleanupBatch = PageRequest.of(0, CLEANUP_BATCH_SIZE);

        List<ProductImage> expiredImages = imageRepository.findByStatusAndCreatedAtBefore(ImageStatus.TEMPORARY,
                cutoff, cleanupBatch);

        for (ProductImage image : expiredImages) {
            imageContentStore.unsetContent(image);
            imageRepository.delete(image);
        }

        List<UserAvatar> expiredAvatars = avatarRepository.findByStatusAndCreatedAtBefore(ImageStatus.TEMPORARY,
                cutoff, cleanupBatch);

        for (UserAvatar avatar : expiredAvatars) {
            avatarContentStore.unsetContent(avatar);
            avatarRepository.delete(avatar);
        }
    }
}
