package com.salob.food_service.api.eatery;

import com.salob.food_service.api._domain.EateryClosureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EateryClosureFlagRepository extends JpaRepository<EateryClosureFlag, UUID> {

    boolean existsByEateryIdAndFlaggerId(UUID eateryId, UUID flaggerId);
}
