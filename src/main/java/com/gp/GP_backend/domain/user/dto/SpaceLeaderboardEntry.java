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
public class SpaceLeaderboardEntry {

    private Integer rank;

    private UUID userId;
    private String fullName;

    private Integer xpPoints;

    private Short level;

    private Integer postsInSpace;

    private Integer answersInSpace;

    private Integer materialsSharedInSpace;
}