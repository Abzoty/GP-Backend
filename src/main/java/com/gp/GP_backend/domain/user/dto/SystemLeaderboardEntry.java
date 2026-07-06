package com.gp.GP_backend.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLeaderboardEntry {

    private Integer rank;

    private UUID userId;
    private String fullName;

    private Integer xpPoints;

    private Short level;

    private Integer totalPosts;

    private Integer totalAnswers;

    private Integer totalMaterialsShared;
}