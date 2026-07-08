package com.example.media.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.media.models.UserAvatar;
import com.example.shared.common.types.ImageStatus;

@Repository
public interface AvatarRepository extends MongoRepository<UserAvatar, UUID> {

    UserAvatar findByUserId(String userId);

    List<UserAvatar> findByStatusAndCreatedAtBefore(ImageStatus status, LocalDateTime cutoff, Pageable pageable);
}
