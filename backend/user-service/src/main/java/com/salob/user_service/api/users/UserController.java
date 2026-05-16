package com.salob.user_service.api.users;

import com.salob.user_service.api.users.dto.MeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(
           @RequestHeader("X-User-Id") UUID id
//           @RequestHeader("X-User-Name") String username,
//           @RequestHeader("X-User-Roles") String roles // Comma-separated string
    ) {
        return ResponseEntity.ok(userService.me(id));
    }
}
