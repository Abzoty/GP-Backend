package com.gp.GP_backend.domain.referencedata.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.gp.GP_backend.domain.referencedata.service.ReferenceDataService;
import com.gp.GP_backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes reference data endpoints for the course catalog, grade mapping,
 * and department-selection questionnaire.
 *
 * These are read-only endpoints, typically called during app initialization
 * by the frontend to populate dropdowns, lookup tables, and the questionnaire
 * flow.
 * Each endpoint returns the full content of its underlying JSON reference file,
 * unchanged in shape, so the frontend can consume it as it sees fit.
 *
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/reference-data")
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    /**
     * Returns the complete course catalog, exactly as stored in course-catalog.json
     * (catalog version, last-updated date, and the full list of courses).
     *
     * @return 200 OK with the course catalog
     */
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<ReferenceDataService.CourseCatalogWrapper>> getCourses() {
        ReferenceDataService.CourseCatalogWrapper catalog = referenceDataService.getCourseCatalog();
        return ResponseEntity.ok(ApiResponse.ok("Course catalog retrieved", catalog));
    }

    /**
     * Returns the grade mapping scale, exactly as stored in grade-mapping.json
     * (version, last-updated date, and the full min/max/grade/points scale).
     *
     * @return 200 OK with the grade mapping structure
     */
    @GetMapping("/grades")
    public ResponseEntity<ApiResponse<ReferenceDataService.GradeMappingWrapper>> getGradeMapping() {
        ReferenceDataService.GradeMappingWrapper mapping = referenceDataService.getGradeMapping();
        return ResponseEntity.ok(ApiResponse.ok("Grade mapping retrieved", mapping));
    }

}