package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Records a course a user has registered for in a given semester.
 *
 * <p>
 * Used by the recommendation engine to suggest relevant spaces and materials
 * based on what courses the user is currently taking or has taken.
 */
@Entity
@Table(
    name = "courses_registered",
    uniqueConstraints = @UniqueConstraint(name = "uk_course_registration_period", columnNames = {
        "user_id", "course_code", "academic_year", "semester"
    }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRegistered {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UNIQUEIDENTIFIER", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** e.g. "CS301" — used to match against space course codes. */
    @Column(name = "course_code", nullable = false, length = 30)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    /** Semester number within the academic year (1 or 2). */
    private Short semester;

    @Column(name = "academic_year")
    private Short academicYear;

    /** Final grade, null if the course is still in progress. */
    @Column(length = 5)
    private String grade;

    @Column(name = "result", precision = 4, scale = 1)
    private BigDecimal result;
    /**
     * True if the student is currently enrolled in this course.
     * False for historical/completed courses.
     */
    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = true;
}
