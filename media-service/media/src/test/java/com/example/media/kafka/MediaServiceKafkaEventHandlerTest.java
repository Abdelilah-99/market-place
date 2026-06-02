package com.example.media.kafka;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.media.models.ProductImage;
import com.example.media.models.UserAvatar;
import com.example.media.services.AvatarService;
import com.example.media.services.ProductImageService;
import com.example.media.services.ProductService;
import com.example.media.services.UserService;
import com.example.shared.common.kafka.dtos.media.KafkaConfirmAvatarEvent;
import com.example.shared.common.kafka.dtos.media.KafkaConfirmImageEvent;
import com.example.shared.common.kafka.dtos.products.KafkaProductCreatedEvent;
import com.example.shared.common.kafka.dtos.products.KafkaProductRemovedEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserCreatedEvent;
import com.example.shared.common.kafka.dtos.users.KafkaUserRemovedEvent;

@ExtendWith(MockitoExtension.class)
class MediaServiceKafkaEventHandlerTest {
    @Mock
    private UserService userService;

    @Mock
    private AvatarService avatarService;

    @Mock
    private ProductService productService;

    @Mock
    private ProductImageService productImageService;

    @InjectMocks
    private UserEvents userEvents;

    @InjectMocks
    private AvatarEvents avatarEvents;

    @InjectMocks
    private ProductEvents productEvents;

    @InjectMocks
    private ProductImagesEvents productImagesEvents;

    private String userId;
    private UUID productId;
    private UUID imageId;
    private UUID avatarId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        productId = UUID.randomUUID();
        imageId = UUID.randomUUID();
        avatarId = UUID.randomUUID();
    }

    @Test
    void testUserCreatedEventListening() {
        KafkaUserCreatedEvent event = new KafkaUserCreatedEvent(userId, "Test User", null);

        userEvents.listenCreateUser(event);

        verify(userService).createUser(event);
    }

    @Test
    void testUserRemovedEventListening() {
        KafkaUserRemovedEvent event = new KafkaUserRemovedEvent(userId);

        userEvents.listenRemoveUser(event);

        verify(avatarService).deleteAvatarByUserId(userId);
        verify(userService).deleteUser(event);
    }

    @Test
    void testProductCreatedEventListening() {
        KafkaProductCreatedEvent event = new KafkaProductCreatedEvent(productId, userId);

        productEvents.listenCreateproduct(event);

        verify(productService).createProduct(event);
    }

    @Test
    void testProductRemovedEventListening() {
        KafkaProductRemovedEvent event = new KafkaProductRemovedEvent(productId);

        productEvents.listenRemoveproduct(event);

        verify(productImageService).deleteProductImageByProductId(productId);
        verify(productService).deleteProduct(event);
    }

    @Test
    void testAvatarConfirmEventListening() {
        KafkaConfirmAvatarEvent event = new KafkaConfirmAvatarEvent(avatarId);

        avatarEvents.listenConfirmAvatar(event);

        verify(avatarService).confirmAvatar(avatarId);
    }

    @Test
    void testAvatarDeleteEventListening() {
        UserAvatar avatar = new UserAvatar();
        avatar.setId(avatarId);

        when(avatarService.getAvatarbyId(avatarId)).thenReturn(avatar);

        KafkaConfirmAvatarEvent event = new KafkaConfirmAvatarEvent(avatarId);

        avatarEvents.listenDeleteAvatar(event);

        verify(avatarService).getAvatarbyId(avatarId);
        verify(avatarService).deleteAvatar(avatar);
    }

    @Test
    void testProductImageConfirmEventListening() {
        KafkaConfirmImageEvent event = new KafkaConfirmImageEvent(imageId);

        productImagesEvents.listenConfirmAvatar(event);

        verify(productImageService).confirmImage(imageId);
    }

    @Test
    void testProductImageDeleteEventListening() {
        ProductImage image = new ProductImage();
        image.setId(imageId);

        when(productImageService.getAvatarbyId(imageId)).thenReturn(image);

        KafkaConfirmImageEvent event = new KafkaConfirmImageEvent(imageId);

        productImagesEvents.listenDeleteAvatar(event);

        verify(productImageService).getAvatarbyId(imageId);
        verify(productImageService).deleteImage(image);
    }

    @Test
    void testMultipleEventProcessing() {
        KafkaUserCreatedEvent userEvent = new KafkaUserCreatedEvent(userId, "User", null);
        KafkaProductCreatedEvent productEvent = new KafkaProductCreatedEvent(productId, userId);
        KafkaConfirmAvatarEvent avatarEvent = new KafkaConfirmAvatarEvent(avatarId);

        userEvents.listenCreateUser(userEvent);
        productEvents.listenCreateproduct(productEvent);
        avatarEvents.listenConfirmAvatar(avatarEvent);

        verify(userService).createUser(userEvent);
        verify(productService).createProduct(productEvent);
        verify(avatarService).confirmAvatar(avatarId);
    }
}
