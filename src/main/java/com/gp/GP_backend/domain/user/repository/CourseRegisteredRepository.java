package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.CourseRegistered;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link CourseRegistered} entities.
 */
public interface CourseRegisteredRepository extends JpaRepository<CourseRegistered, UUID> {

    /**
     * All active enrollments for a user — used by the "current courses" endpoint.
     */
    List<CourseRegistered> findByUserIdAndIsCurrentTrue(UUID userId);

    /** Full enrollment history for a user, both current and archived. */
    List<CourseRegistered> findByUserId(UUID userId);

    /**
     * Duplicate guard: checks whether a user is already registered for a specific
     * course code in a given academic year and semester.
     */
    boolean existsByUserIdAndCourseCodeAndAcademicYearAndSemester(
            UUID userId, String courseCode, Short academicYear, Short semester);
}