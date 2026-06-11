package com.gp.GP_backend.domain.recommendation.client;

import com.gp.GP_backend.domain.recommendation.dto.SpaceRankingResponse;
import com.gp.GP_backend.domain.recommendation.dto.SpaceRecommendationRankRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PythonRecommendationClient {

    private static final ParameterizedTypeReference<List<SpaceRankingResponse>> RANKING_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient mlRestClient;

    public List<SpaceRankingResponse> rankSpaces(SpaceRecommendationRankRequest request) {
        List<SpaceRankingResponse> response = mlRestClient.post()
                .uri("/api/recommendations/spaces/rank")
                .body(request)
                .retrieve()
                .body(RANKING_LIST_TYPE);

        return response == null ? List.of() : response;
    }
}