package com.salob.user_service.api.auth;

import com.salob.user_service.api.auth.dto.LoginRequest;
import com.salob.user_service.api.auth.dto.LoginResponse;
import com.salob.user_service.api.auth.dto.RegisterRequest;
import com.salob.user_service.api._domain.Role;
import com.salob.user_service.api._domain.User;
import com.salob.user_service.api.users.RoleRepository;
import com.salob.user_service.api.users.UserRepository;
import com.salob.user_service.api.users.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtService jwtService;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest req) {
        String loginFailureMessage = "Invalid username/email or password";

        Optional<User> user = userRepo.findByUsernameOrEmail(req.usernameOrEmail(), req.usernameOrEmail());
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, loginFailureMessage);
        }

        boolean doPasswordsMatch = passwordEncoder.matches(req.password(), user.get().getPasswordHash());
        if (!doPasswordsMatch) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, loginFailureMessage);
        }

        String jwt = jwtService.createJwt(user.get())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, loginFailureMessage)
        );
        return new LoginResponse(jwt);
    }

    public void register(RegisterRequest req) {
        if (userRepo.existsByEmailOrUsername(req.email(), req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email or username already exists");
        }
        Role contributorRole = roleRepo.findByLabel(UserRole.CONTRIBUTOR.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        Set<Role> roles = new HashSet<>();
        roles.add(contributorRole);
        var user = User.builder()
                .username(req.username())
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .roles(roles)
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepo.save(user);
    }
}
