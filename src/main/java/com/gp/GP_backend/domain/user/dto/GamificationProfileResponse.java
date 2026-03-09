package com.gp.GP_backend.domain.user.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class GamificationProfileResponse {
    private Long userId;
    private Integer xpPoints;
    private Short level;
    private Integer totalPosts;
    private Integer totalAnswers;
    private Integer totalUpvotesReceived;
    private Integer totalMaterialsShared;
    private Short currentStreakDays;
    private Short longestStreakDays;
    private LocalDate lastActivityDate;
}
