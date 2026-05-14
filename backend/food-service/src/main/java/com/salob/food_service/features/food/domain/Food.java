package com.salob.food_service.features.food.domain;

import com.salob.food_service.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

import lombok.*;

@Getter
@Setter
@Builder
@Entity
@Table(name = "foods")
@NoArgsConstructor
@AllArgsConstructor
public class Food extends BaseEntity {

    @NotBlank
    @Column(unique = true, nullable = false)
    private String label; // e.g., "Chicken Rice"

    @Column(name = "photo_obj_key")
    private String photoObjKey = "";

    @Builder.Default
    @OneToMany(mappedBy = "food", fetch = FetchType.LAZY)
    private List<FoodEntry> foodEntries = new ArrayList<>();
}
