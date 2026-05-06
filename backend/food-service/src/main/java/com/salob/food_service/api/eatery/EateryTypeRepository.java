package com.salob.food_service.api.eatery;

import com.salob.food_service.domain.EateryType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EateryTypeRepository extends JpaRepository<EateryType, UUID> {
    Optional<EateryType> findByLabel(String label);

    boolean existsByLabel(String label);
}
