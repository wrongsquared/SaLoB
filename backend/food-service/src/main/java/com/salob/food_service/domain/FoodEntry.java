package com.salob.food_service.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@Table(name = "food_entries")
@NoArgsConstructor
@AllArgsConstructor
public class FoodEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id", nullable = false)
    private Food food;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eatery_id", nullable = false)
    private Eatery eatery;

    @Column(name = "sg_cents", updatable = false, nullable = false)
    private int sgCents;

    @Builder.Default
    @Column(name = "upvote_count", nullable = false)
    private int upvoteCount = 0;

    @Builder.Default
    @Column(name = "downvote_count", nullable = false)
    private int downvoteCount = 0;

    @Column(name = "submitter_id", updatable = false, nullable = false)
    private UUID submitterId;

    @Builder.Default
    @OneToMany(mappedBy = "foodEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FoodEntryMedia> media = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "foodEntry", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FoodEntryVote> votes = new ArrayList<>();
}
