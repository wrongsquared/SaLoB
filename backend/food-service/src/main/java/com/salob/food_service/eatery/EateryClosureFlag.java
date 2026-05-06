package com.salob.food_service.eatery;

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
@Table(name = "eatery_closure_flags")
public class EateryClosureFlag extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eatery_id", nullable = false)
    private Eatery eatery;

    @Column(name = "flagger_id")
    private UUID flaggerId;
}
