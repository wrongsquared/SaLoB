package com.salob.food_service.features.eatery.dto;
import lombok.Builder;

import java.util.List;

/**
 *
 * @param name - The name of the food item
 * @param sgCentsConsensusPrice - Using our 'Confidence' algorithm, the price which is most like
 *                                to be the correct one. (Is included within the 'pricePoints' array)
 * @param address - The address of the eatery for this food entry
 * @param pricePoints - All price points (which also includes the 'sgCentsConsensusPrice')
 */
@Builder
public record FoodDetailedDTO(
    String name,
    int sgCentsConsensusPrice,
    String address,
    List<PricePoint> pricePoints
) {}
