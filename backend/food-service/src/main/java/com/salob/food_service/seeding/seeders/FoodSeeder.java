package com.salob.food_service.seeding.seeders;

import com.salob.food_service.api.food.FoodRepository;
import com.salob.food_service.domain.Food;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FoodSeeder {
    private final FoodRepository foodRepository;

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
            Food food = Food.builder().label(foodLabel).build();
            foods.add(food);
        });
        return foodRepository.saveAll(foods);
    }
}
