package com.salob.food_service.api.eatery_type;

import java.util.List;
import java.util.UUID;

import com.salob.food_service.api._domain.EateryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EateryTypeRepository extends JpaRepository<EateryType, UUID> {
	<T> List<T> findBy(Class<T> type);
	boolean existsByLabel(String label);
}
