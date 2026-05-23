package com.salob.user_service.api.users;

import com.salob.user_service.api._domain.User;
import com.salob.user_service.api.auth.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
	Optional<User> findByUsernameOrEmail(String username, String email);
	boolean existsByEmailOrUsername(String email, String username);
	Optional<User> findByEmail(String email);
	Optional<User> findByAuthProviderAndProviderId(AuthProvider authProvider, String providerId);
}
