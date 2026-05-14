package com.salob.food_service.seeding;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SeedImageHelper {
    public static final String FOOD_PREFIX = "food-images";
    public static final String EATERY_PREFIX = "eatery-images";
    public static final Path STATIC_ROOT = Paths.get("src/main/resources/static");

    /**
     * Input:  "Chicken Rice"
     * Output: "chicken_rice.jpg"
     */
    public String toJpgFileName(String assetName) {
        String normalized = assetName == null ? "" : assetName.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        if (normalized.isEmpty()) {
            normalized = "unknown";
        }
        return normalized + ".jpg";
    }

    /**
     * Input:  prefix="food-images", fileName="chicken_rice.jpg"
     * Output: "food-images/chicken_rice.jpg"
     */
    public String toObjectKey(String prefix, String fileName) {
        return prefix + "/" + fileName;
    }

    /**
     * Input:  prefix="eatery-images", fileName="maxwell_food_centre.jpg"
     * Output: "src/main/resources/static/eatery-images/maxwell_food_centre.jpg"
     */
    public Path toDiskPath(String prefix, String fileName) {
        return STATIC_ROOT.resolve(prefix).resolve(fileName);
    }

    public boolean isImageOnDisk(Path filePath) {
        return Files.exists(filePath);
    }


    /**
     * Input: BufferedImage + disk path
     * Output: true when written as jpg, false otherwise.
     */
    public boolean saveImageToDisk(BufferedImage image, Path targetPath) {
        if (image == null) {
            return false;
        }
        try {
            Files.createDirectories(targetPath.getParent());
            BufferedImage rgbImage = toRgb(image);
            try (OutputStream out = Files.newOutputStream(targetPath)) {
                return ImageIO.write(rgbImage, "jpg", out);
            }
        } catch (Exception e) {
            log.warn("Failed to save image to disk {}: {}", targetPath, e.getMessage());
            return false;
        }
    }

    private BufferedImage toRgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_RGB) {
            return source;
        }
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return rgb;
    }
}

