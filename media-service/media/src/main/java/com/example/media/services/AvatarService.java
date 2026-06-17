package com.example.media.services;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.media.models.UserAvatar;
import com.example.media.repositories.AvatarRepository;
import com.example.media.stores.UserAvatarContentStore;
import com.example.shared.common.types.ImageStatus;

@Service
public class AvatarService {

    private final AvatarRepository repository;
    private final UserAvatarContentStore contentStore;

    public AvatarService(AvatarRepository repository,
            UserAvatarContentStore contentStore) {
        this.repository = repository;
        this.contentStore = contentStore;
    }

    public boolean isImageMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }

        return mimeType.toLowerCase().startsWith("image/");
    }

    @Transactional
    public UserAvatar uploadAvatar(InputStream inputStream, String mimeType, String userId) {
        // Create metadata
        UserAvatar avatar = new UserAvatar();
        avatar.setId(UUID.randomUUID());
        avatar.setMimeType(mimeType);
        avatar.setUserId(userId);

        // Save metadata first
        avatar = repository.save(avatar);

        // Save actual file bytes to filesystem via Spring Content
        contentStore.setContent(avatar, inputStream);

        // Save again in case contentStore updates entity
        avatar = repository.save(avatar);

        return avatar;
    }

    @Transactional
    public void deleteAvatarByUserId(String userId) {
        UserAvatar avatar = repository.findByUserId(userId);
        if (avatar == null) {
            return; // nothing to delete
        }

        // Delete file content
        contentStore.unsetContent(avatar);

        // Delete DB record
        repository.delete(avatar);
    }

    public UserAvatar getAvatarbyId(UUID id) {
        return repository.findById(id).orElse(null);
    }

    public boolean hasReadableContent(UserAvatar avatar) {
        if (avatar == null) {
            return false;
        }

        try (InputStream ignored = contentStore.getContent(avatar)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public void deleteAvatar(UserAvatar avatar) {
        if (avatar == null) {
            return;
        }

        // Delete file content
        contentStore.unsetContent(avatar);

        // Delete DB record
        repository.delete(avatar);
    }

    @Transactional
    public void confirmAvatar(UUID id) {
        repository.findById(id).ifPresent(avatar -> {
            avatar.setStatus(ImageStatus.LINKED);
            repository.save(avatar);
        });
    }
}
