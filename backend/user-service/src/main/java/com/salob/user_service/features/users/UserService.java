package com.salob.user_service.features.users;

import com.salob.user_service.features.User;
import com.salob.user_service.features.users.dto.MeResponse;
import com.salob.user_service.features.users.dto.WtfScoreItem;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepo;

    public List<UUID> getAllUserIDs() {
        return userRepo.findAll().stream().map(User::getId).toList();
    }

    public MeResponse me(UUID id) {
        return userRepo.findById(id)
                .map(user -> MeResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public double getUserWtfScore(UUID userId) {
        return userRepo.findById(userId)
                .map(User::getWtfScore)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }



    public List<WtfScoreItem> getUserWtfScoreBatch(List<UUID> userIds) {
        return userRepo.findAllById(userIds)
                .stream()
                .map(user -> new WtfScoreItem(user.getId(), user.getWtfScore()))
                .toList();
    }
}
