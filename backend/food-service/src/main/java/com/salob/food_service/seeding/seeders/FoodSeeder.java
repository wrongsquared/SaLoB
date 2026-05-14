package com.salob.food_service.seeding.seeders;

import com.salob.food_service.features.food.FoodRepository;
import com.salob.food_service.features.food.domain.Food;
import com.salob.food_service.seeding.SeedImageHelper;
import com.salob.food_service.storage.minio.MinioStorageService;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FoodSeeder {
    private final FoodRepository foodRepo;
    private final SeedImageHelper seedImageHelper;
    private final MinioStorageService minioStorageService;

    public List<Food> seed() {
        var foods = new ArrayList<Food>();
        List.of(
                "Chicken Rice",
                "Hokkien Mee",
                "Char Kway Teow",
                "Laksa",
                "Nasi Lemak",
                "Roti Prata",
                "Satay",
                "Beef Rendang",
                "Chili Crab",
                "Popiah",
                "Bak Kut Teh",
                "Mee Goreng",
                "Prawn Noodle",
                "Fish Soup",
                "Biryani",
                "Ramen",
                "Sushi",
                "Tacos",
                "Burgers",
                "Pasta",
                "Paella",
                "Ceviche",
                "Falafel",
                "Quiche",
                "Dim Sum",
                "Peking Duck",
                "Kway Teow",
                "Oyster Omelette",
                "Gado Gado",
                "Fried Chicken",
                "Chow Mein",
                "Mango Sticky Rice",
                "Kimchi",
                "Curry Laksa",
                "Spaghetti Bolognese",
                "Tiramisu",
                "Chocolate Cake",
                "Apple Pie",
                "Crepes",
                "Shakshuka",
                "Thai Green Curry",
                "Banh Mi",
                "Pho",
                "Baba Ganoush",
                "Bratwurst",
                "Samosas",
                "Chowder",
                "Jambalaya",
                "Buffalo Wings",
                "Eggs Benedict",
                "Caesar Salad",
                "Gelato",
                "Pavlova",
                "Baklava",
                "Donuts",
                "Croissants",
                "Bagels",
                "Fettuccine Alfredo",
                "Coconut Curry",
                "Moussaka",
                "Risotto",
                "Tandoori Chicken"
        ).forEach(foodLabel -> {
            if (foodRepo.existsByLabel(foodLabel)) {
                return;
            }

            String imgFilename = seedImageHelper.toJpgFileName(foodLabel);
            String imgObjKey = seedImageHelper.toObjectKey(SeedImageHelper.FOOD_PREFIX, imgFilename);
            Path imgPathOnDisk = seedImageHelper.toDiskPath(SeedImageHelper.FOOD_PREFIX, imgFilename);

            if (!minioStorageService.objectExists(imgObjKey)) {
                if (!seedImageHelper.isImageOnDisk(imgPathOnDisk)) {
                    throw new IllegalStateException(
                        "Missing food seed image at " + imgPathOnDisk +
                        ". Run scripts/seed_images.py --type food to generate images."
                    );
                }

                String uploadedKey = minioStorageService.uploadImage(imgPathOnDisk, imgObjKey);
                if (uploadedKey == null) {
                    throw new IllegalStateException(
                        "Failed to upload food seed image to MinIO: " + imgPathOnDisk
                    );
                }
                imgObjKey = uploadedKey;
            }

            foods.add(Food.builder().label(foodLabel).photoObjKey(imgObjKey).build());
        });
        return foodRepo.saveAll(foods);
    }
}
