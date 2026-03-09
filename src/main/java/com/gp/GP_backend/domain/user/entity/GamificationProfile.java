package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gamification_profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GamificationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "xp_points")
    @Builder.Default
    private Integer xpPoints = 0;

    @Builder.Default
    private Short level = 1;

    @Column(name = "total_posts")
    @Builder.Default
    private Integer totalPosts = 0;

    @Column(name = "total_answers")
    @Builder.Default
    private Integer totalAnswers = 0;

    @Column(name = "total_upvotes_received")
    @Builder.Default
    private Integer totalUpvotesReceived = 0;

    @Column(name = "total_materials_shared")
    @Builder.Default
    private Integer totalMaterialsShared = 0;

    @Column(name = "current_streak_days")
    @Builder.Default
    private Short currentStreakDays = 0;

    @Column(name = "longest_streak_days")
    @Builder.Default
    private Short longestStreakDays = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
