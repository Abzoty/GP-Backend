package com.gp.GP_backend.domain.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceRecommendationRankRequest {

    private UserContext user;
    private List<CandidateSpace> candidateSpaces;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserContext {
        private UUID id;
        private List<String> courses;
        private List<JoinedSpace> joinedSpaces;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinedSpace {
        private UUID id;
        private String title;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandidateSpace {
        private UUID id;
        private String title;
        private String description;
        private Integer memberCount;
        private LocalDate lastActivityDate;
    }
}