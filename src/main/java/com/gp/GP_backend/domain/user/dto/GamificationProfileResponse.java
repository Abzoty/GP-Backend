package com.gp.GP_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamificationProfileResponse {

    private UUID userId;
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
