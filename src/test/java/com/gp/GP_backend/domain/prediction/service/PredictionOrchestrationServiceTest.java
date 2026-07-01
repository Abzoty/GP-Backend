package com.gp.GP_backend.domain.prediction.service;

import com.gp.GP_backend.domain.course.entity.CourseRegistration;
import com.gp.GP_backend.domain.course.repository.CourseRegistrationRepository;
import com.gp.GP_backend.domain.prediction.dto.CourseDataDto;
import com.gp.GP_backend.domain.prediction.dto.PredictionRequest;
import com.gp.GP_backend.domain.prediction.dto.PredictionResponse;
import com.gp.GP_backend.domain.prediction.dto.PythonServiceResponse;
import com.gp.GP_backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PredictionOrchestrationServiceTest {

    @Mock
    private PredictionServiceClient predictionServiceClient;

    @Mock
    private CourseRegistrationRepository courseRegistrationRepository;

    @InjectMocks
    private PredictionOrchestrationService service;

    @Test
    void predictShouldCombineQuestionnaireAndModelScoresWhenModelIsAvailable() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);

        when(courseRegistrationRepository.findByUser(user)).thenReturn(List.of(
                courseRegistration("CS101", new BigDecimal("30"), new BigDecimal("50"), new BigDecimal("80"), "A", new BigDecimal("4.0"))));
        when(predictionServiceClient.predict(any())).thenReturn(PythonServiceResponse.builder()
                .probabilities(Map.of(
                        "CS", new BigDecimal("0.10"),
                        "Computer Science", new BigDecimal("0.30"),
                        "IS", new BigDecimal("0.80")))
                .modelVersion("v1.2")
                .build());

        PredictionResponse response = service.predict(user, PredictionRequest.builder()
                .normalizedScores(Map.of(
                        "CS", new BigDecimal("0.80"),
                        "IS", new BigDecimal("0.20")))
                .build());

        assertThat(response.isModelAvailable()).isTrue();
        assertThat(response.getModelVersion()).isEqualTo("v1.2");
        assertThat(response.getWarning()).isNull();
        assertThat(response.getTopDepartment()).isEqualTo("Computer Science");
        assertThat(response.getWeights().getQuestionnaire()).isEqualByComparingTo("0.5");
        assertThat(response.getWeights().getModel()).isEqualByComparingTo("0.5");
        assertThat(response.getDepartmentScores()).extracting(PredictionResponse.DepartmentScore::getDepartment)
                .containsExactly("Computer Science", "Information Systems");

        PredictionResponse.DepartmentScore first = response.getDepartmentScores().get(0);
        assertThat(first.getQuestionnaireScore()).isEqualByComparingTo("0.8000");
        assertThat(first.getModelScore()).isEqualByComparingTo("0.4000");
        assertThat(first.getCombinedScore()).isEqualByComparingTo("0.6000");

        ArgumentCaptor<com.gp.GP_backend.domain.prediction.dto.PythonServiceRequest> requestCaptor =
                ArgumentCaptor.forClass(com.gp.GP_backend.domain.prediction.dto.PythonServiceRequest.class);
        verify(predictionServiceClient).predict(requestCaptor.capture());
        List<CourseDataDto> courseData = requestCaptor.getValue().getCourses();
        assertThat(courseData).hasSize(1);
        assertThat(courseData.get(0).getCode()).isEqualTo("CS101");
        assertThat(courseData.get(0).getResult()).isEqualByComparingTo("80");
    }

    @Test
    void predictShouldFallBackToQuestionnaireOnlyWhenModelIsUnavailable() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);

        when(courseRegistrationRepository.findByUser(user)).thenReturn(List.of(
                courseRegistration("CS101", new BigDecimal("30"), new BigDecimal("50"), new BigDecimal("80"), "A", new BigDecimal("4.0"))));
        when(predictionServiceClient.predict(any())).thenReturn(null);

        PredictionResponse response = service.predict(user, PredictionRequest.builder()
                .normalizedScores(Map.of(
                        "CS", new BigDecimal("0.80"),
                        "IS", new BigDecimal("0.20")))
                .build());

        assertThat(response.isModelAvailable()).isFalse();
        assertThat(response.getModelVersion()).isNull();
        assertThat(response.getWarning()).isNotBlank();
        assertThat(response.getTopDepartment()).isEqualTo("Computer Science");
        assertThat(response.getWeights().getQuestionnaire()).isEqualByComparingTo("1.0");
        assertThat(response.getWeights().getModel()).isEqualByComparingTo("0.0");
        assertThat(response.getDepartmentScores()).extracting(PredictionResponse.DepartmentScore::getDepartment)
                .containsExactly("Computer Science", "Information Systems");
        assertThat(response.getDepartmentScores().get(0).getModelScore()).isNull();
        assertThat(response.getDepartmentScores().get(0).getCombinedScore()).isEqualByComparingTo("0.8000");
    }

    private static User user(UUID id) {
        return User.builder()
                .id(id)
                .email("student@example.com")
                .passwordHash("hash")
                .fullName("Student Name")
                .build();
    }

    private static CourseRegistration courseRegistration(String code, BigDecimal termWork, BigDecimal examWork,
            BigDecimal result, String grade, BigDecimal points) {
        return CourseRegistration.builder()
                .code(code)
                .termWork(termWork)
                .examWork(examWork)
                .result(result)
                .grade(grade)
                .points(points)
                .build();
    }
}