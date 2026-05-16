package com.salob.user_service.api._domain;

import com.salob.user_service.api.auth.AuthProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_provider", columnList = "auth_provider, provider_id")
})
public class User {
    @Id
    @ToString.Include
    @JdbcTypeCode(SqlTypes.UUID)
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Email // Validates email format automatically
    @NotBlank
    @Column(name = "email", unique = true, nullable = true, length = 150)
    private String email;

    @Column(name = "password_hash") // Nullable for OAuth
    private String passwordHash;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @NotNull
    @Enumerated(EnumType.STRING) // Saves "GOOGLE" or "LOCAL" as string in DB
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    @Column(name = "provider_id") // Unique ID from Google/GitHub
    private String providerId;

    @Column(name = "avatar_obj_key")
    private String avatarObjKey;

    // WTF Stats
    @Builder.Default // Ensures Builder doesn't overwrite default values
    @Column(name = "wtf_score", nullable = false)
    private double wtfScore = 50.0;

    @Builder.Default
    @Column(name = "total_submissions", nullable = false)
    private int totalSubmissions = 0;

    @Builder.Default
    @Column(name = "upvotes_received", nullable = false)
    private int upvotesReceived = 0;

    @Builder.Default
    @Column(name = "downvotes_received", nullable = false)
    private int downvotesReceived = 0;

    @Builder.Default
    @Column(name = "anomalies_flagged", nullable = false)
    private int anomaliesFlagged = 0;

    @CreatedDate // Auto-set by Spring on first insert
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate // Useful to track when WTF score was last recalculated
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Use only ID for equality to prevent issues with Proxies/Sets
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
