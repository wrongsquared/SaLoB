package com.salob.food_service.api.food_entry_flag;

import com.salob.food_service.api._domain.FoodEntryFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FoodEntryFlagRepository extends JpaRepository<FoodEntryFlag, UUID> {}
