package com.salob.food_service.seeding.seeders;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.salob.food_service.features.eatery.EateryRepository;
import com.salob.food_service.features.eatery.domain.Eatery;
import com.salob.food_service.features.eatery.domain.EateryType;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class EaterySeeder {
    private record EaterySeedSpec(String name, String address, String typeLabel, double lat, double lon) {}

    private final EateryRepository eateryRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Transactional
    public List<Eatery> seed(List<EateryType> eateryTypes) {
        Map<String, EateryType> typesByLabel = eateryTypes
            .stream()
            .collect(Collectors.toMap(EateryType::getLabel, type -> type));

        if (typesByLabel.isEmpty()) {
            throw new IllegalStateException("Cannot seed eateries before eatery types exist");
        }

        List<EaterySeedSpec> eateries = List.of(
                // --- CENTRAL / CBD ---
                new EaterySeedSpec("Maxwell Food Centre", "1 Maxwell Road", "Hawker Stall", 1.2803, 103.8447),
                new EaterySeedSpec("Amoy Street Food Centre", "2 Amoy Street", "Hawker Stall", 1.2792, 103.8466),
                new EaterySeedSpec("Lau Pa Sat", "18 Raffles Quay", "Food Court", 1.2808, 103.8504),
                new EaterySeedSpec("Common Man Coffee Roasters", "22 Martin Road", "Cafe", 1.2913, 103.8384),
                new EaterySeedSpec("Tiong Bahru Bakery", "56 Eng Hoon Street", "Bakery", 1.2852, 103.8333),
                new EaterySeedSpec("PS.Cafe at Ann Siang Hill", "45 Ann Siang Road", "Restaurant", 1.2818, 103.8457),
                new EaterySeedSpec("Ya Kun Kaya Toast (Far East Square)", "7 Chatterton Road", "Kopitiam", 1.2835, 103.8483),
                new EaterySeedSpec("The Daily Cut (Tanjong Pagar)", "15 Tanjong Pagar Road", "Bistro", 1.2778, 103.8463),
                new EaterySeedSpec("Heytea (ION Orchard)", "2 Orchard Turn", "Bubble Tea Shop", 1.3040, 103.8320),
                new EaterySeedSpec("Din Tai Fung (Paragon)", "290 Orchard Road", "Restaurant", 1.3039, 103.8358),
                // --- EAST (Foodie Haven) ---
                new EaterySeedSpec("Old Airport Road Food Centre", "51 Old Airport Road", "Hawker Stall", 1.3083, 103.8858),
                new EaterySeedSpec("East Coast Lagoon Food Village", "1220 East Coast Parkway", "Hawker Stall", 1.3075, 103.9351),
                new EaterySeedSpec("Penny University", "402 East Coast Road", "Cafe", 1.3056, 103.9052),
                new EaterySeedSpec("328 Katong Laksa", "328 Katong Road", "Restaurant", 1.3044, 103.9064),
                new EaterySeedSpec("Beach Road Prawn Noodle", "1 Beach Road", "Restaurant", 1.3115, 103.9112),
                new EaterySeedSpec("Birds of Paradise Gelato", "H28, 63 Aung San Avenue", "Dessert Shop", 1.3038, 103.9065),
                new EaterySeedSpec("Changi Village Hawker Centre", "2 Changi Village Road", "Hawker Stall", 1.3891, 103.9882),
                new EaterySeedSpec("Bedok 85 (Fengshan Market)", "85 Bedok North Street 4", "Hawker Stall", 1.3321, 103.9382),
                new EaterySeedSpec("Simpang Bedok", "6 Bedok Road", "Kopitiam", 1.3313, 103.9478),
                new EaterySeedSpec("Jewel Changi Food Court", "78 Airport Boulevard", "Food Court", 1.3602, 103.9898),
                // --- WEST ---
                new EaterySeedSpec("Ghim Moh Market", "20 Ghim Moh Road", "Hawker Stall", 1.3110, 103.7883),
                new EaterySeedSpec("Clementi 448 Market", "448 Clementi Avenue 3", "Hawker Stall", 1.3134, 103.7644),
                new EaterySeedSpec("Creamier Handcrafted Ice Cream", "5 Clementi Road", "Dessert Shop", 1.2801, 103.8033),
                new EaterySeedSpec("The Workbench Bistro", "20 Upper Bukit Timah Road", "Bistro", 1.3142, 103.7651),
                new EaterySeedSpec("Holland Village Food Centre", "4 Holland Avenue", "Food Court", 1.3113, 103.7951),
                new EaterySeedSpec("Boon Lay Power Nasi Lemak", "222 Boon Lay Place", "Hawker Stall", 1.3450, 103.7128),
                new EaterySeedSpec("Waku Waku Burger", "9008 Waku Waku Road", "Restaurant", 1.3315, 103.7483),
                new EaterySeedSpec("Jurong Point Kopitiam", "1 Jurong West Central 2", "Kopitiam", 1.3396, 103.7067),
                new EaterySeedSpec("Atlas Coffeehouse", "6 Bukit Timah Road", "Cafe", 1.3224, 103.8074),
                new EaterySeedSpec("Burnt Cones (Sunset Way)", "77 Sunset Way", "Dessert Shop", 1.3235, 103.7598),
                // --- NORTH / NORTH-EAST ---
                new EaterySeedSpec("Chomp Chomp Food Centre", "20 Kensington Park Road", "Hawker Stall", 1.3551, 103.8665),
                new EaterySeedSpec("Serangoon Garden Market", "49A Serangoon Garden Way", "Hawker Stall", 1.3555, 103.8667),
                new EaterySeedSpec("Wheeler's Estate", "2 Park Lane", "Bistro", 1.3918, 103.8761),
                new EaterySeedSpec("Sembawang Eating House", "10 Sembawang Road", "Kopitiam", 1.4481, 103.8211),
                new EaterySeedSpec("Chong Pang Market", "101 Yishun Avenue 1", "Hawker Stall", 1.4315, 103.8278),
                new EaterySeedSpec("Nakhon Kitchen (Kovan)", "204 Hougang Street 21", "Restaurant", 1.3601, 103.8850),
                new EaterySeedSpec("Lola's Cafe", "5 Simon Road", "Cafe", 1.3603, 103.8860),
                new EaterySeedSpec("Punggol Settlement Seafood", "2 Punggol Point Road", "Restaurant", 1.4069, 103.9238),
                new EaterySeedSpec("Whampoa Drive Food Centre", "2 Whampoa Drive", "Hawker Stall", 1.3232, 103.8546),
                new EaterySeedSpec("Upper Thomson Prata House", "32 Upper Thomson Road", "Restaurant", 1.3524, 103.8341),
                // --- GENERIC CHAIN ENTRIES FOR VOLUME ---
                new EaterySeedSpec("McDonald's (Ang Mo Kio)", "700 Ang Mo Kio Avenue 6", "Fast Food", 1.3695, 103.8485),
                new EaterySeedSpec("KFC (Tampines Hub)", "2 Tampines Avenue 5", "Fast Food", 1.3531, 103.9402),
                new EaterySeedSpec("Koi The (Plaza Singapura)", "68 Orchard Road", "Bubble Tea Shop", 1.3007, 103.8454),
                new EaterySeedSpec("LiHo (VivoCity)", "1 HarbourFront Walk", "Bubble Tea Shop", 1.2642, 103.8223),
                new EaterySeedSpec("Starbucks (Rochester Park)", "9 Rochester Drive", "Cafe", 1.3048, 103.7885),
                new EaterySeedSpec("Toast Box (Nex)", "23 Serangoon Central", "Kopitiam", 1.3506, 103.8723),
                new EaterySeedSpec("BreadTalk (Bugis Junction)", "200 Victoria Street", "Bakery", 1.3002, 103.8561),
                new EaterySeedSpec("Cedele (Raffles City)", "252 North Bridge Road", "Bakery", 1.2938, 103.8532),
                new EaterySeedSpec("Haidilao (Clarke Quay)", "3 Riverside Road", "Restaurant", 1.2902, 103.8461),
                new EaterySeedSpec("Food Republic (Wisma Atria)", "435 Orchard Road", "Food Court", 1.3035, 103.8335),
                // --- ADDING MORE RANDOMIZED NEIGHBOURHOOD GEMS ---
                new EaterySeedSpec("Circuit Road Food Centre", "73 Circuit Road", "Hawker Stall", 1.3256, 103.8867),
                new EaterySeedSpec("Geylang Bahru Market", "38 Geylang Bahru", "Hawker Stall", 1.3215, 103.8711),
                new EaterySeedSpec("Kim Keat Hokkien Mee", "7 Kim Keat Road", "Hawker Stall", 1.3305, 103.8552),
                new EaterySeedSpec("Toa Payoh Lucky Pisang Raja", "4 Toa Payoh Central", "Dessert Shop", 1.3322, 103.8475),
                new EaterySeedSpec("Sin Ming Roti Prata", "688 Sin Ming Road", "Hawker Stall", 1.3542, 103.8309),
                new EaterySeedSpec("Heavens (Ghim Moh)", "24 Ghim Moh Link", "Hawker Stall", 1.3108, 103.7881),
                new EaterySeedSpec("Zam Zam Singapore", "19 Jalan Kledek", "Restaurant", 1.3022, 103.8587),
                new EaterySeedSpec("Victory Restaurant", "5 Serangoon North Avenue 1", "Restaurant", 1.3021, 103.8585),
                new EaterySeedSpec("Swee Choon Tim Sum", "183 Thomson Road", "Restaurant", 1.3082, 103.8568),
                new EaterySeedSpec("The Whale Tea (Lot One)", "2 Choa Chu Kang Avenue 4", "Bubble Tea Shop", 1.3852, 103.7475),
                new EaterySeedSpec("Jollibean (Toa Payoh)", "9 Toa Payoh Lorong 1", "Dessert Shop", 1.3328, 103.8486),
                new EaterySeedSpec("Old Hen Coffee Bar", "18 Tai Thong Crescent", "Cafe", 1.3184, 103.8495),
                new EaterySeedSpec("Chye Seng Huat Hardware", "150 Tyrwhitt Road", "Cafe", 1.3119, 103.8603),
                new EaterySeedSpec("Brunetti (Tanglin Mall)", "8 Tanglin Road", "Cafe", 1.3049, 103.8242),
                new EaterySeedSpec("Cedele Bakery Cafe", "161 Upper Thomson Road", "Bakery", 1.3092, 103.9298),
                new EaterySeedSpec("Plain Vanilla Bakery", "37 Eng Hoon Street", "Bakery", 1.2811, 103.8418),
                new EaterySeedSpec("Tiong Bahru Galicier Pastry", "23 Tiong Bahru Road", "Bakery", 1.2848, 103.8325),
                new EaterySeedSpec("Brotherbird Bakehouse", "40 Arab Street", "Bakery", 1.3015, 103.8592),
                new EaterySeedSpec("Two Men Bagel House", "25 Circular Road", "Bistro", 1.2758, 103.8448),
                new EaterySeedSpec("Wild Honey", "22 Scott Road", "Restaurant", 1.3024, 103.8351),
                new EaterySeedSpec("Genki Sushi (Ngee Ann City)", "391 Orchard Road", "Restaurant", 1.3024, 103.8340),
                new EaterySeedSpec("Sushi Tei (Holland V)", "22 Holland Avenue", "Restaurant", 1.3118, 103.7960),
                new EaterySeedSpec("PastaMania (Clementi Mall)", "3155 Commonwealth Avenue", "Fast Food", 1.3150, 103.7641),
                new EaterySeedSpec("Saizeriya (Liang Court)", "177 River Valley Road", "Restaurant", 1.2913, 103.8445),
                new EaterySeedSpec("Marche Movenpick", "3A Level 1, 9 Raffles Boulevard", "Restaurant", 1.3011, 103.8394),
                new EaterySeedSpec("Encik Tan (Bedok Mall)", "311 Bedok North Road", "Hawker Stall", 1.3248, 103.9304),
                new EaterySeedSpec("Stuff'd (Bugis Junction)", "200 Victoria Street", "Fast Food", 1.3005, 103.8559),
                new EaterySeedSpec("Irvins Salted Egg (Orchard)", "2 Orchard Turn", "Dessert Shop", 1.3038, 103.8332),
                new EaterySeedSpec("Nine Fresh", "503 Jurong West Street 51", "Dessert Shop", 1.3508, 103.8725),
                new EaterySeedSpec("Milksha (Suntec)", "3 Temasek Boulevard", "Bubble Tea Shop", 1.2935, 103.8572)
        );
        eateries.forEach(spec -> seedEateryIfMissing(spec, typesByLabel, new Random()));

        return eateryRepository.findAll();
    }

    private void seedEateryIfMissing(EaterySeedSpec spec, Map<String, EateryType> typesByLabel, Random random) {
        if (eateryRepository.existsByName(spec.name())) {
            return;
        }

        EateryType type = typesByLabel.get(spec.typeLabel());
        if (type == null) {
            throw new IllegalStateException("Missing eatery type: " + spec.typeLabel());
        }

        boolean isOpen = random.nextDouble() < 0.8; // 80% chance the eatery is open
        eateryRepository.save(
            Eatery.builder()
                    .name(spec.name())
                    .type(type)
                    .address(spec.address())
                    .isOpen(isOpen)
                    .location(geometryFactory.createPoint(new Coordinate(spec.lon, spec.lat)))
                    .build()
        );
    }
}
