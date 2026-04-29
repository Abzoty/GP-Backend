package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks a user's gamification state: XP, level, activity streaks, and
 * aggregate counters.
 *
 * <p>
 * One-to-one with {@link User}. Created automatically when a new user registers
 * (handled in
 * {@link com.gp.GP_backend.domain.user.service.GamificationService}).
 */
@Entity
@Table(name = "gamification_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamificationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    /** Back-reference to the owner; unique ensures 1-to-1. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /** Total experience points accumulated across all actions. */
    @Column(name = "xp_points")
    @Builder.Default
    private Integer xpPoints = 0;

    /** Derived from XP thresholds. Level 1 is the starting level. */
    @Builder.Default
    private Short level = 1;

    /** Lifetime count of posts created by this user. */
    @Column(name = "total_posts")
    @Builder.Default
    private Integer totalPosts = 0;

    /** Lifetime count of answers submitted by this user. */
    @Column(name = "total_answers")
    @Builder.Default
    private Integer totalAnswers = 0;

    /** Lifetime upvotes received across all answers. */
    @Column(name = "total_upvotes_received")
    @Builder.Default
    private Integer totalUpvotesReceived = 0;

    /** Lifetime materials shared in spaces. */
    @Column(name = "total_materials_shared")
    @Builder.Default
    private Integer totalMaterialsShared = 0;

    /** Consecutive days the user has been active. Resets on inactivity. */
    @Column(name = "current_streak_days")
    @Builder.Default
    private Short currentStreakDays = 0;

    /** The best streak ever recorded for this user. */
    @Column(name = "longest_streak_days")
    @Builder.Default
    private Short longestStreakDays = 0;

    /** Date of the most recent activity; used for streak calculation. */
    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Optimistic-lock version to prevent lost updates under concurrent XP writes.
     */
    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
