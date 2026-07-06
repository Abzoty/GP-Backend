package com.gp.GP_backend.domain.recommendation.dto;

import com.gp.GP_backend.domain.space.dto.SpaceResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceRecommendationResponse {

    private SpaceResponse space;

    private int methodCount;

    private double score;

    private Set<String> reasons;
}
