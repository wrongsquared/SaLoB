package com.salob.user_service.storage.minio;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioStorageService {
    private final MinioClient minioClient;
    private final MinioProperties properties;

    public String uploadImage(Path imagePath, String objectKey) {
        createBucketIfNotExists(properties.getBucket());
        try (InputStream inputStream = Files.newInputStream(imagePath)) {
            long size = Files.size(imagePath);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .stream(inputStream, size, (long) -1)
                            .contentType("image/jpeg")
                            .build()
            );
            return objectKey;
        } catch (Exception e) {
            log.warn("MinIO upload failed for {}: {}", objectKey, e.getMessage());
            return null;
        }
    }

    public boolean objectExists(String objectKey) {
        createBucketIfNotExists(properties.getBucket());
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getPresignedUrl(String objectKey, Duration expiry) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        createBucketIfNotExists(properties.getBucket());
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .expiry((int) expiry.toSeconds())
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to create presigned URL for {}: {}", objectKey, e.getMessage());
            return null;
        }
    }

    private void createBucketIfNotExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to ensure MinIO bucket exists", e);
        }
    }
}
