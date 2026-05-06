package com.salob.food_service.seeding.seeders;

import com.salob.food_service.eatery.Eatery;
import com.salob.food_service.eatery.EateryType;
import com.salob.food_service.eatery.repositories.EateryRepository;
import com.salob.food_service.eatery.repositories.EateryTypeRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class EaterySeeder {

    private static final double MIN_LATITUDE = 1.2166;
    private static final double MAX_LATITUDE = 1.4784;
    private static final double MIN_LONGITUDE = 103.6065;
    private static final double MAX_LONGITUDE = 104.0437;

    private final EateryRepository eateryRepository;
    private final EateryTypeRepository eateryTypeRepository;
    private final Random random = new SecureRandom();
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public void seed() {
        Map<String, EateryType> typesByLabel = eateryTypeRepository
            .findAll()
            .stream()
            .collect(Collectors.toMap(EateryType::getLabel, type -> type));

        if (typesByLabel.isEmpty()) {
            throw new IllegalStateException("Cannot seed eateries before eatery types exist");
        }

        List<EaterySeedSpec> eateries = List.of(
                // --- CENTRAL / CBD ---
                new EaterySeedSpec("Maxwell Food Centre", "Hawker Stall", 1.2803, 103.8447),
                new EaterySeedSpec("Amoy Street Food Centre", "Hawker Stall", 1.2792, 103.8466),
                new EaterySeedSpec("Lau Pa Sat", "Food Court", 1.2808, 103.8504),
                new EaterySeedSpec("Common Man Coffee Roasters", "Cafe", 1.2913, 103.8384),
                new EaterySeedSpec("Tiong Bahru Bakery", "Bakery", 1.2852, 103.8333),
                new EaterySeedSpec("PS.Cafe at Ann Siang Hill", "Restaurant", 1.2818, 103.8457),
                new EaterySeedSpec("Ya Kun Kaya Toast (Far East Square)", "Kopitiam", 1.2835, 103.8483),
                new EaterySeedSpec("The Daily Cut (Tanjong Pagar)", "Bistro", 1.2778, 103.8463),
                new EaterySeedSpec("Heytea (ION Orchard)", "Bubble Tea Shop", 1.3040, 103.8320),
                new EaterySeedSpec("Din Tai Fung (Paragon)", "Restaurant", 1.3039, 103.8358),

                // --- EAST (Foodie Haven) ---
                new EaterySeedSpec("Old Airport Road Food Centre", "Hawker Stall", 1.3083, 103.8858),
                new EaterySeedSpec("East Coast Lagoon Food Village", "Hawker Stall", 1.3075, 103.9351),
                new EaterySeedSpec("Penny University", "Cafe", 1.3056, 103.9052),
                new EaterySeedSpec("328 Katong Laksa", "Restaurant", 1.3044, 103.9064),
                new EaterySeedSpec("Beach Road Prawn Noodle", "Restaurant", 1.3115, 103.9112),
                new EaterySeedSpec("Birds of Paradise Gelato", "Dessert Shop", 1.3038, 103.9065),
                new EaterySeedSpec("Changi Village Hawker Centre", "Hawker Stall", 1.3891, 103.9882),
                new EaterySeedSpec("Bedok 85 (Fengshan Market)", "Hawker Stall", 1.3321, 103.9382),
                new EaterySeedSpec("Simpang Bedok", "Kopitiam", 1.3313, 103.9478),
                new EaterySeedSpec("Jewel Changi Food Court", "Food Court", 1.3602, 103.9898),

                // --- WEST ---
                new EaterySeedSpec("Ghim Moh Market", "Hawker Stall", 1.3110, 103.7883),
                new EaterySeedSpec("Clementi 448 Market", "Hawker Stall", 1.3134, 103.7644),
                new EaterySeedSpec("Creamier Handcrafted Ice Cream", "Dessert Shop", 1.2801, 103.8033),
                new EaterySeedSpec("The Workbench Bistro", "Bistro", 1.3142, 103.7651),
                new EaterySeedSpec("Holland Village Food Centre", "Food Court", 1.3113, 103.7951),
                new EaterySeedSpec("Boon Lay Power Nasi Lemak", "Hawker Stall", 1.3450, 103.7128),
                new EaterySeedSpec("Waku Waku Burger", "Restaurant", 1.3315, 103.7483),
                new EaterySeedSpec("Jurong Point Kopitiam", "Kopitiam", 1.3396, 103.7067),
                new EaterySeedSpec("Atlas Coffeehouse", "Cafe", 1.3224, 103.8074),
                new EaterySeedSpec("Burnt Cones (Sunset Way)", "Dessert Shop", 1.3235, 103.7598),

                // --- NORTH / NORTH-EAST ---
                new EaterySeedSpec("Chomp Chomp Food Centre", "Hawker Stall", 1.3551, 103.8665),
                new EaterySeedSpec("Serangoon Garden Market", "Hawker Stall", 1.3555, 103.8667),
                new EaterySeedSpec("Wheeler's Estate", "Bistro", 1.3918, 103.8761),
                new EaterySeedSpec("Sembawang Eating House", "Kopitiam", 1.4481, 103.8211),
                new EaterySeedSpec("Chong Pang Market", "Hawker Stall", 1.4315, 103.8278),
                new EaterySeedSpec("Nakhon Kitchen (Kovan)", "Restaurant", 1.3601, 103.8850),
                new EaterySeedSpec("Lola's Cafe", "Cafe", 1.3603, 103.8860),
                new EaterySeedSpec("Punggol Settlement Seafood", "Restaurant", 1.4069, 103.9238),
                new EaterySeedSpec("Whampoa Drive Food Centre", "Hawker Stall", 1.3232, 103.8546),
                new EaterySeedSpec("Upper Thomson Prata House", "Restaurant", 1.3524, 103.8341),

                // --- GENERIC CHAIN ENTRIES FOR VOLUME ---
                new EaterySeedSpec("McDonald's (Ang Mo Kio)", "Fast Food", 1.3695, 103.8485),
                new EaterySeedSpec("KFC (Tampines Hub)", "Fast Food", 1.3531, 103.9402),
                new EaterySeedSpec("Koi The (Plaza Singapura)", "Bubble Tea Shop", 1.3007, 103.8454),
                new EaterySeedSpec("LiHo (VivoCity)", "Bubble Tea Shop", 1.2642, 103.8223),
                new EaterySeedSpec("Starbucks (Rochester Park)", "Cafe", 1.3048, 103.7885),
                new EaterySeedSpec("Toast Box (Nex)", "Kopitiam", 1.3506, 103.8723),
                new EaterySeedSpec("BreadTalk (Bugis Junction)", "Bakery", 1.3002, 103.8561),
                new EaterySeedSpec("Cedele (Raffles City)", "Bakery", 1.2938, 103.8532),
                new EaterySeedSpec("Haidilao (Clarke Quay)", "Restaurant", 1.2902, 103.8461),
                new EaterySeedSpec("Food Republic (Wisma Atria)", "Food Court", 1.3035, 103.8335),

                // --- ADDING MORE RANDOMIZED NEIGHBOURHOOD GEMS ---
                new EaterySeedSpec("Circuit Road Food Centre", "Hawker Stall", 1.3256, 103.8867),
                new EaterySeedSpec("Geylang Bahru Market", "Hawker Stall", 1.3215, 103.8711),
                new EaterySeedSpec("Kim Keat Hokkien Mee", "Hawker Stall", 1.3305, 103.8552),
                new EaterySeedSpec("Toa Payoh Lucky Pisang Raja", "Dessert Shop", 1.3322, 103.8475),
                new EaterySeedSpec("Sin Ming Roti Prata", "Hawker Stall", 1.3542, 103.8309),
                new EaterySeedSpec("Heavens (Ghim Moh)", "Hawker Stall", 1.3108, 103.7881),
                new EaterySeedSpec("Zam Zam Singapore", "Restaurant", 1.3022, 103.8587),
                new EaterySeedSpec("Victory Restaurant", "Restaurant", 1.3021, 103.8585),
                new EaterySeedSpec("Swee Choon Tim Sum", "Restaurant", 1.3082, 103.8568),
                new EaterySeedSpec("The Whale Tea (Lot One)", "Bubble Tea Shop", 1.3852, 103.7475),
                new EaterySeedSpec("Jollibean (Toa Payoh)", "Dessert Shop", 1.3328, 103.8486),
                new EaterySeedSpec("Old Hen Coffee Bar", "Cafe", 1.3184, 103.8495),
                new EaterySeedSpec("Chye Seng Huat Hardware", "Cafe", 1.3119, 103.8603),
                new EaterySeedSpec("Brunetti (Tanglin Mall)", "Cafe", 1.3049, 103.8242),
                new EaterySeedSpec("Cedele Bakery Cafe", "Bakery", 1.3092, 103.9298),
                new EaterySeedSpec("Plain Vanilla Bakery", "Bakery", 1.2811, 103.8418),
                new EaterySeedSpec("Tiong Bahru Galicier Pastry", "Bakery", 1.2848, 103.8325),
                new EaterySeedSpec("Brotherbird Bakehouse", "Bakery", 1.3015, 103.8592),
                new EaterySeedSpec("Two Men Bagel House", "Bistro", 1.2758, 103.8448),
                new EaterySeedSpec("Wild Honey", "Restaurant", 1.3024, 103.8351),
                new EaterySeedSpec("Genki Sushi (Ngee Ann City)", "Restaurant", 1.3024, 103.8340),
                new EaterySeedSpec("Sushi Tei (Holland V)", "Restaurant", 1.3118, 103.7960),
                new EaterySeedSpec("PastaMania (Clementi Mall)", "Fast Food", 1.3150, 103.7641),
                new EaterySeedSpec("Saizeriya (Liang Court)", "Restaurant", 1.2913, 103.8445),
                new EaterySeedSpec("Marche Movenpick", "Restaurant", 1.3011, 103.8394),
                new EaterySeedSpec("Encik Tan (Bedok Mall)", "Hawker Stall", 1.3248, 103.9304),
                new EaterySeedSpec("Stuff'd (Bugis Junction)", "Fast Food", 1.3005, 103.8559),
                new EaterySeedSpec("Irvins Salted Egg (Orchard)", "Dessert Shop", 1.3038, 103.8332),
                new EaterySeedSpec("Nine Fresh", "Dessert Shop", 1.3508, 103.8725),
                new EaterySeedSpec("Milksha (Suntec)", "Bubble Tea Shop", 1.2935, 103.8572)
        );
        eateries.forEach(spec -> seedEateryIfMissing(spec, typesByLabel));
    }

    private void seedEateryIfMissing(EaterySeedSpec spec, Map<String, EateryType> typesByLabel) {
        if (eateryRepository.existsByName(spec.name())) {
            return;
        }

        EateryType type = typesByLabel.get(spec.typeLabel());
        if (type == null) {
            throw new IllegalStateException("Missing eatery type: " + spec.typeLabel());
        }

        eateryRepository.save(
            Eatery.builder()
                    .name(spec.name())
                    .type(type)
                    .location(geometryFactory.createPoint(new Coordinate(spec.lon, spec.lat)))
                    .build()
        );
    }

    private Point getRandomSingaporeLocation() {
        double longitude = MIN_LONGITUDE + random.nextDouble() * (MAX_LONGITUDE - MIN_LONGITUDE);
        double latitude = MIN_LATITUDE + random.nextDouble() * (MAX_LATITUDE - MIN_LATITUDE);

        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }

    private record EaterySeedSpec(String name, String typeLabel, double lat, double lon) {}
}
