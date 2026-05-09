package com.salob.user_service.features.users;

import com.salob.user_service.features.users.dto.MeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@RequestHeader UUID id) {
        MeResponse res = userService.me(id);
        return ResponseEntity.ok(res);
    }
}
