package com.gp.GP_backend.domain.referencedata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceDataServiceTest {

    @Test
    void shouldLoadReferenceDataAndResolveKnownValues() {
        ReferenceDataService service = new ReferenceDataService(new ObjectMapper(), new DefaultResourceLoader());

        ReferenceDataService.CourseCatalogWrapper catalog = service.getCourseCatalog();
        ReferenceDataService.GradeMappingWrapper mapping = service.getGradeMapping();

        assertThat(catalog).isNotNull();
        assertThat(catalog.getCourses()).isNotEmpty();
        assertThat(mapping).isNotNull();
        assertThat(mapping.getScale()).isNotEmpty();

        ReferenceDataService.CourseData firstCourse = catalog.getCourses().get(0);
        assertThat(service.findCourse(firstCourse.getCode())).isPresent();
        assertThat(service.getAllCourses()).hasSize(catalog.getCourses().size());

        ReferenceDataService.GradeRangeData firstRange = mapping.getScale().get(0);
        BigDecimal score = BigDecimal.valueOf((firstRange.getMinScore() + firstRange.getMaxScore()) / 2.0);

        assertThat(service.resolveGrade(score)).isEqualTo(firstRange.getGrade());
        assertThat(service.resolvePoints(score)).isEqualByComparingTo(BigDecimal.valueOf(firstRange.getPoints()));
    }

    @Test
    void shouldResolveNullValuesToDefaults() {
        ReferenceDataService service = new ReferenceDataService(new ObjectMapper(), new DefaultResourceLoader());

        assertThat(service.resolveGrade(null)).isEqualTo("F");
        assertThat(service.resolvePoints(null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(service.findCourse("UNKNOWN")).isEmpty();
    }
}