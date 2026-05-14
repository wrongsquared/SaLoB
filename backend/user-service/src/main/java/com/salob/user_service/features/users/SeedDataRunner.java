package com.salob.user_service.features.users;

import com.salob.user_service.features.User;
import com.salob.user_service.features.auth.AuthProvider;
import com.salob.user_service.storage.minio.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;

@Slf4j
@Component
@Profile("dev")
@org.jspecify.annotations.NullMarked
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {
    private final UserRepository userRepository;
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

                var random = new Random();
                User user = User.builder()
                        .username(username)
                        .email(username.toLowerCase() + "@salob.com")
                        .passwordHash(passwordEncoder.encode("password"))
                        .role(UserRole.CONTRIBUTOR)
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
}
