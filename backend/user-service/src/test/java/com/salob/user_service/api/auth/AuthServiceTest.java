package com.salob.user_service.api.auth;

import com.salob.user_service.api.auth.dto.*;
import com.salob.user_service.api._domain.Role;
import com.salob.user_service.api._domain.User;
import com.salob.user_service.api.users.RoleRepository;
import com.salob.user_service.api.users.UserRepository;
import com.salob.user_service.api.users.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepo;
    @Mock private RoleRepository roleRepo;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(jwtService, userRepo, roleRepo, passwordEncoder);
    }

    // =========================================================================
    // SECTION 1: login
    // =========================================================================

    @Test
    void login_withValidCredentials_returnsJwt() {
        String email = "test@example.com";
        String password = "password123";
        String jwtToken = "fake-jwt-token";

        User user = User.builder()
                .email(email)
                .username("testuser")
                .passwordHash("encoded-password")
                .authProvider(AuthProvider.LOCAL)
                .build();
        user.setId(UUID.randomUUID());

        when(userRepo.findByUsernameOrEmail(email, email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, "encoded-password")).thenReturn(true);
        when(jwtService.createJwt(user)).thenReturn(Optional.of(jwtToken));

        LoginResponse result = authService.login(new LoginRequest(email, password));

        assertEquals(jwtToken, result.jwt());
        verify(userRepo).findByUsernameOrEmail(email, email);
        verify(passwordEncoder).matches(password, "encoded-password");
        verify(jwtService).createJwt(user);
    }

    @Test
    void login_withUnknownUser_throws400() {
        when(userRepo.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("unknown@test.com", "password")));
    }

    @Test
    void login_withWrongPassword_throws400() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encoded-password")
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(userRepo.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("test@example.com", "wrong-password")));
    }

    // =========================================================================
    // SECTION 2: register
    // =========================================================================

    @Test
    void register_withNewEmail_createsUser() {
        String email = "new@example.com";
        String username = "newuser";
        String password = "password123";

        Role contributorRole = Role.builder().label(UserRole.CONTRIBUTOR.name()).build();
        contributorRole.setId(UUID.randomUUID());

        when(userRepo.existsByEmailOrUsername(email, username)).thenReturn(false);
        when(roleRepo.findByLabel(UserRole.CONTRIBUTOR.name())).thenReturn(Optional.of(contributorRole));
        when(passwordEncoder.encode(password)).thenReturn("encoded");

        authService.register(new RegisterRequest(email, username, password));

        verify(userRepo).save(any(User.class));
        verify(userRepo).existsByEmailOrUsername(email, username);
        verify(roleRepo).findByLabel(UserRole.CONTRIBUTOR.name());
    }

    @Test
    void register_withExistingEmailOrUsername_throws409() {
        when(userRepo.existsByEmailOrUsername(anyString(), anyString())).thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> authService.register(new RegisterRequest("existing@test.com", "existing", "password")));
    }
}
