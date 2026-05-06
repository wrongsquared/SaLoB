package com.salob.food_service.eatery.repositories;

import com.salob.food_service.eatery.Eatery;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EateryRepository extends JpaRepository<Eatery, UUID> {
    boolean existsByName(String name);
}
