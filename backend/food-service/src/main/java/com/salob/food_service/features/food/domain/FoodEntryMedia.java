package com.salob.food_service.features.food.domain;

import com.salob.food_service.common.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "food_entry_media")
public class FoodEntryMedia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_entry_id", nullable = false)
    private FoodEntry foodEntry;

    @Column(name = "uploader_id", updatable = false, nullable = false)
    private UUID uploaderId;

    @Column(name = "object_key", updatable = false, nullable = false)
    private String objectKey;
}
