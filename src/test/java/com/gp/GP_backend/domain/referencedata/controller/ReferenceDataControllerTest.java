package com.gp.GP_backend.domain.referencedata.controller;

import com.gp.GP_backend.domain.referencedata.service.ReferenceDataService;
import com.gp.GP_backend.shared.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferenceDataControllerTest {

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private ReferenceDataController controller;

    @Test
    void getCoursesShouldReturnCourseCatalogEnvelope() {
        ReferenceDataService.CourseCatalogWrapper catalog = new ReferenceDataService.CourseCatalogWrapper();
        catalog.setVersion("v1");
        catalog.setUpdated("2026-01-01");
        ReferenceDataService.CourseData course = new ReferenceDataService.CourseData();
        course.setCode("CS101");
        course.setName("Algorithms");
        catalog.setCourses(List.of(course));

        when(referenceDataService.getCourseCatalog()).thenReturn(catalog);

        ResponseEntity<ApiResponse<ReferenceDataService.CourseCatalogWrapper>> entity = controller.getCourses();

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().isSuccess()).isTrue();
        assertThat(entity.getBody().getData().getVersion()).isEqualTo("v1");
        assertThat(entity.getBody().getData().getCourses()).hasSize(1);
    }

    @Test
    void getGradeMappingShouldReturnGradeMappingEnvelope() {
        ReferenceDataService.GradeMappingWrapper mapping = new ReferenceDataService.GradeMappingWrapper();
        mapping.setVersion("v1");
        mapping.setUpdatedAt("2026-01-01");

        when(referenceDataService.getGradeMapping()).thenReturn(mapping);

        ResponseEntity<ApiResponse<ReferenceDataService.GradeMappingWrapper>> entity = controller.getGradeMapping();

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getMessage()).isEqualTo("Grade mapping retrieved");
        assertThat(entity.getBody().getData().getVersion()).isEqualTo("v1");
    }
}