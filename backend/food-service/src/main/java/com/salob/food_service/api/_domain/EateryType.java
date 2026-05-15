package com.salob.food_service.api._domain;

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
@Table(name = "eatery_types")
@NoArgsConstructor
@AllArgsConstructor
public class EateryType extends BaseEntity {

    @NotBlank
    @Column(unique = true, nullable = false)
    private String label; // e.g., "Hawker Stall"

    @Builder.Default
    @OneToMany(mappedBy = "type", fetch = FetchType.LAZY)
    private List<Eatery> eateries = new ArrayList<>();
}
