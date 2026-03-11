package com.gp.GP_backend.domain.recommendation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fetches course recommendations from the ML service based on a user's profile.
 *
 * TODO: Implement using the mlRestClient bean.
 * The ML service expects the user's registered courses and academic info.
 */
@Service
@RequiredArgsConstructor
public class CourseRecommendationService {
    // TODO: inject RestClient mlRestClient
}
