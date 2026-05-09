package com.salob.user_service.features.auth;

import com.salob.user_service.features.auth.dto.LoginRequest;
import com.salob.user_service.features.auth.dto.LoginResponse;
import com.salob.user_service.features.auth.dto.RegisterRequest;
import com.salob.user_service.features.User;
import com.salob.user_service.features.users.UserRepository;
import com.salob.user_service.features.users.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(@RequestBody LoginRequest req) {
        String loginFailureMessage = "Invalid username/email or password";

        Optional<User> user = userRepo.findByUsernameOrEmail(req.usernameOrEmail(), req.usernameOrEmail());
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, loginFailureMessage);
        }

        boolean doPasswordsMatch = passwordEncoder.matches(req.password(), user.get().getPasswordHash());
        if (!doPasswordsMatch) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, loginFailureMessage);
        }

        String jwt = jwtService.issueJwt(user.get())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, loginFailureMessage)
        );
        return new LoginResponse(jwt);
    }

    public void register(@RequestBody RegisterRequest req) {
        if (userRepo.existsByEmailOrUsername(req.email(), req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or username already exists");
        }
        var user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(UserRole.CONTRIBUTOR)
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepo.save(user);
    }
}
