package com.salob.user_service.api.users;

import com.salob.user_service.api._domain.User;
import com.salob.user_service.api.users.dto.MeResponse;
import com.salob.user_service.api.users.dto.WtfScoreItem;
import com.salob.user_service.storage.minio.MinioStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private MinioStorageService minioService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepo, minioService);
    }

    @Test
    void me_returnsUserProfile() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .email("test@example.com")
                .username("testuser")
                .authProvider(com.salob.user_service.api.auth.AuthProvider.LOCAL)
                .avatarObjKey("avatar-key")
                .build();
        user.setId(userId);

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(minioService.getPresignedUrl("avatar-key", Duration.ofMinutes(15)))
                .thenReturn("https://avatar.url");

        MeResponse result = userService.me(userId);

        assertEquals(userId, result.id());
        assertEquals("test@example.com", result.email());
        assertEquals("testuser", result.username());
        assertEquals("https://avatar.url", result.avatarUrl());
    }

    @Test
    void me_whenNotFound_throws404() {
        when(userRepo.findById(any())).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> userService.me(UUID.randomUUID()));
    }

    @Test
    void getUserWtfScore_returnsScore() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().wtfScore(65.0).authProvider(com.salob.user_service.api.auth.AuthProvider.LOCAL).build();
        user.setId(userId);

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));

        double score = userService.getUserWtfScore(userId);
        assertEquals(65.0, score, 1e-9);
    }

    @Test
    void getUserWtfScoreBatch_returnsScoresForFoundUsers() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        User user1 = User.builder().wtfScore(70.0).authProvider(com.salob.user_service.api.auth.AuthProvider.LOCAL).build();
        user1.setId(id1);

        when(userRepo.findAllById(List.of(id1, id2))).thenReturn(List.of(user1));

        List<WtfScoreItem> results = userService.getUserWtfScoreBatch(List.of(id1, id2));
        assertEquals(1, results.size());
        assertEquals(id1, results.getFirst().userId());
        assertEquals(70.0, results.getFirst().wtfScore(), 1e-9);
    }

    @Test
    void getAllUserIDs_returnsAllIds() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        User user1 = User.builder().authProvider(com.salob.user_service.api.auth.AuthProvider.LOCAL).build();
        user1.setId(id1);
        User user2 = User.builder().authProvider(com.salob.user_service.api.auth.AuthProvider.LOCAL).build();
        user2.setId(id2);

        when(userRepo.findAll()).thenReturn(List.of(user1, user2));

        List<UUID> ids = userService.getAllUserIDs();
        assertEquals(2, ids.size());
        assertTrue(ids.contains(id1));
        assertTrue(ids.contains(id2));
    }
}
