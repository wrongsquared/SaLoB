package com.salob.user_service.features.users;

import com.salob.user_service.features.User;
import com.salob.user_service.features.auth.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            userRepository.deleteAllInBatch();
            log.debug("Deleted users");
        } catch (Exception e) {
            log.warn("Failed to delete users: {}", e.getMessage());
        }

        log.info("Starting seeding process...");
        seedUsers();
        log.info("Seeding complete");
    }

    @Transactional
    private void seedUsers() {
        var users = new ArrayList<User>();

        for (int i = 0; i < 30; i++) {
            String username = "user_" + i;

            users.add(
                    User.builder()
                            .username(username)
                            .email(username + "@gmail.com")
                            .passwordHash(passwordEncoder.encode(username))
                            .role(UserRole.CONTRIBUTOR)
                            .authProvider(AuthProvider.LOCAL)
                            .build()
            );
        }
        userRepository.saveAll(users);
    }
}
