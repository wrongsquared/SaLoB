package com.salob.food_service.eatery;

import com.salob.food_service.common.BaseEntity;
import com.salob.food_service.food.FoodEntry;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.locationtech.jts.geom.Point;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "eateries")
public class Eatery extends BaseEntity {
    @NotBlank
    @Column(name = "name",  nullable = false)
    private String name;

    /**
     * SRID 4326 = GPS coordinates (WGS84).
     * `geometry(Point, 4326)` tells PostGIS what shape and coordinate system to use.
     */
    @NotNull
    @Column(name = "location", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", updatable = false, nullable = false)
    private EateryType type;

    @Builder.Default
    @Column(name = "is_closed", nullable = false)
    private boolean isClosed = false;

    @Builder.Default
    @OneToMany(mappedBy = "eatery", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FoodEntry> foodEntries = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "eatery", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EateryClosureFlag> closureFlags = new ArrayList<>();
}
