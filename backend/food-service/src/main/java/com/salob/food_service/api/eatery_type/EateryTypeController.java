package com.salob.food_service.api.eatery_type;

import com.salob.food_service.api._domain.EateryType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/eatery-types")
public class EateryTypeController {

	private final EateryTypeRepository eateryTypeRepo;

	@GetMapping
	public ResponseEntity<List<EateryType>> listEateryTypes() {
		return ResponseEntity.ok(eateryTypeRepo.findAll());
	}
}
