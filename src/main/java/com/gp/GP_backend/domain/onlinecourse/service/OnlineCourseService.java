package com.gp.GP_backend.domain.onlinecourse.service;

import com.gp.GP_backend.domain.onlinecourse.dto.OnlineCourseResponse;
import com.gp.GP_backend.domain.onlinecourse.entity.OnlineCourse;
import com.gp.GP_backend.domain.onlinecourse.repository.OnlineCourseRepository;
import com.gp.GP_backend.domain.course.entity.CourseRegistration;
import com.gp.GP_backend.domain.course.repository.CourseRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineCourseService {

    private final OnlineCourseRepository onlineCourseRepository;
    private final CourseRegistrationRepository courseRegistrationRepository;

    @Transactional(readOnly = true)
    public List<OnlineCourseResponse> getCoursesForStudent(UUID userId) {
        // 1. Get all course codes the student is registered in
        List<String> registeredCodes = courseRegistrationRepository
            .findByUserIdAndClosedFalse(userId).stream()
            .filter(c -> c.getClosed() == false)
            .map(CourseRegistration::getCode)
            .filter(code -> code != null && !code.isBlank())
            .distinct()
            .toList();

        if (registeredCodes.isEmpty()) {
            log.info("User {} has no registered courses — returning empty list.", userId);
            return List.of();
        }

        // 2. Return online courses matching those codes, sorted by score
        return onlineCourseRepository
            .findByCourseCodeInOrderByScoreDesc(registeredCodes)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private OnlineCourseResponse toResponse(OnlineCourse c) {
        return OnlineCourseResponse.builder()
            .id(c.getId())
            .courseCode(c.getCourseCode())
            .courseName(c.getCourseName())
            .source(c.getSource())
            .title(c.getTitle())
            .url(c.getUrl())
            .description(c.getDescription())
            .rating(c.getRating())
            .reviews(c.getReviews())
            .price(c.getPrice())
            .score(c.getScore())
            .lastUpdated(c.getLastUpdated())
            .build();
    }
}