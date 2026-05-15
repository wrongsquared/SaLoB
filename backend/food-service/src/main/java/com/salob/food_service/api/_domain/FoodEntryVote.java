package com.salob.food_service.api._domain;

import com.salob.food_service.common.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "food_entry_votes",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_food_entry_votes_voter_entry", columnNames = { "voter_id", "food_entry_id" }),
    }
)
public class FoodEntryVote extends BaseEntity {
    @Column(name = "voter_id", updatable = false, nullable = false)
    private UUID voterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_entry_id", nullable = false)
    private FoodEntry foodEntry;

    @Column(name = "is_upvote", updatable = false, nullable = false)
    private boolean isUpvote;
}
