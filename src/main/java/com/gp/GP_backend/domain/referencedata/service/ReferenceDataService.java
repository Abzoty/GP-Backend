package com.gp.GP_backend.domain.referencedata.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReferenceDataService {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    // Full, file-shaped representations - returned as-is by the controller.
    private CourseCatalogWrapper courseCatalog;
    private GradeMappingWrapper gradeMapping;

    // Derived lookup structures used internally for resolution logic.
    private Map<String, CourseData> coursesByCode;
    private List<GradeRangeData> gradeRanges;

    public ReferenceDataService(ObjectMapper objectMapper, ResourceLoader resourceLoader) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        loadAndValidateData();
    }

    private void loadAndValidateData() {
        try {
            loadCourseCatalog();
            loadGradeMapping();
            log.info("Reference data loaded successfully. Catalog version: {}, Courses: {}",
                    courseCatalog.getVersion(), coursesByCode.size());
        } catch (IOException e) {
            log.error("Failed to load reference data", e);
            throw new RuntimeException("Reference data initialization failed", e);
        }
    }

    private void loadCourseCatalog() throws IOException {
        String path = "classpath:reference-data/course-catalog.json";
        try (InputStream is = resourceLoader.getResource(path).getInputStream()) {
            courseCatalog = objectMapper.readValue(is, CourseCatalogWrapper.class);

            coursesByCode = courseCatalog.getCourses().stream()
                    .collect(Collectors.toMap(
                            CourseData::getCode,
                            c -> c,
                            (existing, duplicate) -> {
                                throw new IllegalStateException(
                                        "Duplicate course code in catalog: " + existing.getCode());
                            }));

            log.info("Course catalog loaded: {} courses", coursesByCode.size());
        }
    }

    private void loadGradeMapping() throws IOException {
        String path = "classpath:reference-data/grade-mapping.json";
        try (InputStream is = resourceLoader.getResource(path).getInputStream()) {
            gradeMapping = objectMapper.readValue(is, GradeMappingWrapper.class);

            // Keep a separate sorted copy for internal resolution so the
            // original file order is preserved on the wrapper returned to the API.
            gradeRanges = new ArrayList<>(gradeMapping.getScale());
            gradeRanges.sort(Comparator.comparing(GradeRangeData::getMinScore).reversed());

            log.info("Grade mapping loaded: {} grade ranges", gradeRanges.size());
        }
    }


    public Optional<CourseData> findCourse(String code) {
        return Optional.ofNullable(coursesByCode.get(code));
    }

    public String resolveGrade(BigDecimal result) {
        if (result == null) {
            return "F";
        }

        double score = result.doubleValue();
        for (GradeRangeData range : gradeRanges) {
            if (score >= range.getMinScore() && score <= range.getMaxScore()) {
                return range.getGrade();
            }
        }
        return "F";
    }

    public BigDecimal resolvePoints(BigDecimal result) {
        if (result == null) {
            return BigDecimal.ZERO;
        }

        double score = result.doubleValue();
        for (GradeRangeData range : gradeRanges) {
            if (score >= range.getMinScore() && score <= range.getMaxScore()) {
                return BigDecimal.valueOf(range.getPoints());
            }
        }
        return BigDecimal.ZERO;
    }

    public Collection<CourseData> getAllCourses() {
        return coursesByCode.values();
    }

    /**
     * Returns the full course catalog (version, updated date, and courses),
     * exactly as parsed from course-catalog.json.
     */
    public CourseCatalogWrapper getCourseCatalog() {
        return courseCatalog;
    }

    /**
     * Returns the full grade mapping (version, updatedAt, and scale),
     * exactly as parsed from grade-mapping.json.
     */
    public GradeMappingWrapper getGradeMapping() {
        return gradeMapping;
    }


    // ==================== Inner DTOs for JSON deserialization ====================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CourseCatalogWrapper {
        private String version;
        private String updated;
        private List<CourseData> courses;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getUpdated() {
            return updated;
        }

        public void setUpdated(String updated) {
            this.updated = updated;
        }

        public List<CourseData> getCourses() {
            return courses;
        }

        public void setCourses(List<CourseData> courses) {
            this.courses = courses;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GradeMappingWrapper {
        private String version;
        private String updatedAt;
        private List<GradeRangeData> scale;

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public List<GradeRangeData> getScale() {
            return scale;
        }

        public void setScale(List<GradeRangeData> scale) {
            this.scale = scale;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CourseData {
        private String code;
        private String name;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GradeRangeData {
        private String grade;

        @JsonProperty("min")
        private Double minScore;

        @JsonProperty("max")
        private Double maxScore;

        private Double points;

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }

        public Double getMinScore() {
            return minScore;
        }

        public void setMinScore(Double minScore) {
            this.minScore = minScore;
        }

        public Double getMaxScore() {
            return maxScore;
        }

        public void setMaxScore(Double maxScore) {
            this.maxScore = maxScore;
        }

        public Double getPoints() {
            return points;
        }

        public void setPoints(Double points) {
            this.points = points;
        }
    }
}