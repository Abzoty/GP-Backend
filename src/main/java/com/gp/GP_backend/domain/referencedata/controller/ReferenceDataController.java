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

@RestController
@RequestMapping("/api/v1/reference-data")
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/courses")
    public ResponseEntity<ApiResponse<ReferenceDataService.CourseCatalogWrapper>> getCourses() {
        ReferenceDataService.CourseCatalogWrapper catalog = referenceDataService.getCourseCatalog();
        return ResponseEntity.ok(ApiResponse.ok("Course catalog retrieved", catalog));
    }

    @GetMapping("/grades")
    public ResponseEntity<ApiResponse<ReferenceDataService.GradeMappingWrapper>> getGradeMapping() {
        ReferenceDataService.GradeMappingWrapper mapping = referenceDataService.getGradeMapping();
        return ResponseEntity.ok(ApiResponse.ok("Grade mapping retrieved", mapping));
    }

}