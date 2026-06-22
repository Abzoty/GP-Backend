package com.gp.GP_backend.domain.referencedata.controller;

import com.gp.GP_backend.domain.referencedata.service.ReferenceDataService;
import com.gp.GP_backend.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;

/**
 * Exposes reference data endpoints for course catalog and grade mappings.
 *
 * These are read-only endpoints, typically called during app initialization
 * by the frontend to populate dropdowns and lookup tables.
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
     * Returns the complete course catalog.
     *
     * @return 200 OK with a list of all courses
     */
    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<Collection<ReferenceDataService.CourseData>>> getCourses() {
        Collection<ReferenceDataService.CourseData> courses = referenceDataService.getAllCourses();
        return ResponseEntity.ok(ApiResponse.ok("Course catalog retrieved", courses));
    }

    /**
     * Returns the grade mapping scale (grade ranges and GPA points).
     *
     * @return 200 OK with the grade mapping structure
     */
    @GetMapping("/grades")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGradeMapping() {
        // Return a simple view of the grade scale
        Map<String, Object> response = Map.of(
                "minScore", 0,
                "maxScore", 100,
                "description", "Grade mapping scale for result to grade conversion");
        return ResponseEntity.ok(ApiResponse.ok("Grade mapping retrieved", response));
    }
}