package com.salob.user_service.features.auth;

import com.salob.user_service.features.auth.dto.LoginRequest;
import com.salob.user_service.features.auth.dto.RegisterRequest;
import com.salob.user_service.features.users.User;
import com.salob.user_service.features.users.UserRepository;
import com.salob.user_service.features.users.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public ResponseEntity<String> login(@RequestBody LoginRequest req) {
        String loginFailureMessage = "Invalid username/email or password";

        Optional<User> user = userRepo.findByUsernameOrEmail(req.usernameOrEmail(), req.usernameOrEmail());
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginFailureMessage);
        }

        boolean doPasswordsMatch = passwordEncoder.matches(req.password(), user.get().getPasswordHash());
        if (!doPasswordsMatch) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(loginFailureMessage);
        }

        String token = jwtService.issueJwt(user.get()).orElseThrow(() -> new RuntimeException("Failed to issue JWT"));
        return ResponseEntity.ok(token);
    }

    public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
        if (userRepo.existsByEmailOrUsername(req.email(), req.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        var user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(UserRole.CONTRIBUTOR)
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepo.save(user);
        return ResponseEntity.noContent().build();
    }
}
