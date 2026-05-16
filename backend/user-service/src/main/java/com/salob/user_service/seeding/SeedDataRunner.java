package com.salob.user_service.seeding;

import com.salob.user_service.api._domain.Role;
import com.salob.user_service.api._domain.User;
import com.salob.user_service.api.auth.AuthProvider;
import com.salob.user_service.api.users.RoleRepository;
import com.salob.user_service.api.users.UserRepository;
import com.salob.user_service.api.users.UserRole;
import com.salob.user_service.storage.minio.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Slf4j
@Component
@Profile("dev")
@org.jspecify.annotations.NullMarked
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioStorageService minioStorageService;

    @Override
    public void run(String[] args) {
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

    private void seedUsers() {
        ArrayList<User> users = new ArrayList<>();

        try {
            Role contributorRole = getOrCreateRole(UserRole.CONTRIBUTOR.name());
            File imageDir = new ClassPathResource("static/user-images").getFile();
            File[] imageFiles = Objects.requireNonNull(
                    imageDir.listFiles((ignoredDir, name) -> name.toLowerCase().endsWith(".jpg")),
                    "No .jpg user images were found in static/user-images"
            );

            if (imageFiles.length == 0) {
                throw new IllegalStateException("No .jpg user images were found in static/user-images");
            }

            Arrays.sort(imageFiles, Comparator.comparing(File::getName));

            for (File imageFile : imageFiles) {
                String filename = imageFile.getName();
                String username = filename.substring(0, filename.lastIndexOf('.'));
                String objectKey = "user-images/" + filename;

                String uploadedKey = minioStorageService.uploadImage(imageFile.toPath(), objectKey);
                if (uploadedKey == null) {
                    log.error(
                            "Failed to upload seed image '{}' to MinIO as '{}'. Aborting seeding. Likely causes: MinIO is unreachable, credentials are invalid, the bucket cannot be created/accessed, or the file cannot be read.",
                            filename,
                            objectKey
                    );
                    throw new IllegalStateException("MinIO upload failed for seed image: " + filename);
                }

                Random random = new Random();
                Set<Role> roles = new HashSet<>();
                roles.add(contributorRole);
                User user = User.builder()
                        .username(username)
                        .email(username.toLowerCase() + "@salob.com")
                        .passwordHash(passwordEncoder.encode("password"))
                        .roles(roles)
                        .authProvider(AuthProvider.LOCAL)
                        .avatarObjKey(uploadedKey)
                        .wtfScore(random.nextDouble() * 100)
                        .build();
                users.add(user);
            }

            userRepository.saveAll(users);
        } catch (Exception e) {
            log.error("User seeding aborted: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to seed users", e);
        }
    }

    private Role getOrCreateRole(String label) {
        Optional<Role> existingRole = roleRepository.findByLabel(label);
        if (existingRole.isPresent()) {
            return existingRole.get();
        }
        Role newRole = Role.builder()
                .label(label)
                .build();
        return roleRepository.save(newRole);
    }
}
