package com.salob.user_service.api.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.salob.user_service.api._domain.Role;
import com.salob.user_service.api._domain.User;
import com.salob.user_service.api.auth.AuthProvider;
import com.salob.user_service.api.auth.dto.GoogleLoginRequest;
import com.salob.user_service.api.auth.dto.LoginResponse;
import com.salob.user_service.api.users.RoleRepository;
import com.salob.user_service.api.users.UserRepository;
import com.salob.user_service.api.users.UserRole;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

	private final JwtService jwtService;
	private final UserRepository userRepo;
	private final RoleRepository roleRepo;

	@Value("${google.client-id}")
	private String clientId;

	public LoginResponse loginWithGoogle(GoogleLoginRequest req) {
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(null, null)
				.setAudience(Collections.singletonList(clientId)).build();

		GoogleIdToken idToken;
		try {
			idToken = verifier.verify(req.idToken());
		} catch (GeneralSecurityException | IOException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
		}

		if (idToken == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
		}

		Payload payload = idToken.getPayload();

		String email = payload.getEmail();
		String providerId = payload.getSubject();
		String name = (String) payload.get("name");
		String pictureUrl = (String) payload.get("picture");

		if (email == null || providerId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user info from Google");
		}

		// Check if a LOCAL user already has this email
		Optional<User> existingLocalUser = userRepo.findByEmail(email);
		if (existingLocalUser.isPresent() && existingLocalUser.get().getAuthProvider() == AuthProvider.LOCAL) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"This email is already registered with LOCAL auth. Please log in with your password.");
		}

		// Check if user already exists with Google
		Optional<User> existingUser = userRepo.findByAuthProviderAndProviderId(AuthProvider.GOOGLE, providerId);
		if (existingUser.isPresent()) {
			// Existing Google user — login
			return new LoginResponse(jwtService.createJwt(existingUser.get())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Failed to create JWT")));
		}

		// New Google user — register
		String username = generateUsername(name, email);
		Role contributorRole = roleRepo.findByLabel(UserRole.CONTRIBUTOR.name())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
		Set<Role> roles = new HashSet<>();
		roles.add(contributorRole);

		User user = User.builder().username(username).email(email).authProvider(AuthProvider.GOOGLE)
				.providerId(providerId).avatarObjKey(null) // TODO: Download Google avatar to MinIO
				.roles(roles).build();
		userRepo.save(user);

		return new LoginResponse(jwtService.createJwt(user)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Failed to create JWT")));
	}

	private String generateUsername(String name, String email) {
		if (name != null && !name.isBlank()) {
			String base = name.toLowerCase().replaceAll("[^a-z0-9]", "").substring(0, Math.min(name.length(), 20));
			if (!base.isEmpty()) {
				return base;
			}
		}
		// Fallback: use local part of email
		String localPart = email.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
		return localPart.isEmpty() ? "user_" + System.currentTimeMillis() : localPart;
	}
}
