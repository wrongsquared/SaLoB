package com.salob.food_service.api._domain;

import com.salob.food_service.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "food_entry_flags")
public class FoodEntryFlag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_entry_id", nullable = false)
    private FoodEntry foodEntry;

    @Column(name = "flagger_id", nullable = false)
    private UUID flaggerId;

    @Column(name = "reason", nullable = false, length = 512)
    private String reason;
}
