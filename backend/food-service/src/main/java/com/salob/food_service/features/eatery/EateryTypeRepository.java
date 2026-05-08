package com.salob.food_service.features.eatery;

import java.util.Optional;
import java.util.UUID;

import com.salob.food_service.features.eatery.domain.EateryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EateryTypeRepository extends JpaRepository<EateryType, UUID> {
    Optional<EateryType> findByLabel(String label);

    boolean existsByLabel(String label);
}
