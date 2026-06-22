package com.gp.GP_backend.domain.course.entity;

import com.gp.GP_backend.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a user's course registration with derived grade, result, and
 * points.
 *
 * Key differences from the old CourseRegistered:
 * - ID is Long (auto-increment IDENTITY), not UUID
 * - No courseCode/courseName client input; only course code stored
 * - No semester/academicYear fields (redundant with user's current_semester)
 * - grade, result, points are always server-derived, never client input
 * - unique constraint is (user_id, code) — same course only once
 * - termWork (0–40) and examWork (0–60) together compute result = termWork +
 * examWork
 * - closed flag indicates course is finished/graded
 *
 * @since 1.0
 */
@Entity
@Table(name = "course_registrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "code" })
})
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Course code from the catalog (e.g., "CS301").
     * References domain/referencedata/service/ReferenceDataService#coursesByCode.
     */
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    /**
     * Term work score (continuous assessment), typically 0–40.
     * Client supplies this; server never infers it.
     */
    @Column(name = "term_work", nullable = false, precision = 5, scale = 2)
    private BigDecimal termWork;

    /**
     * Exam work score (final exam), typically 0–60.
     * Client supplies this; server never infers it.
     */
    @Column(name = "exam_work", nullable = false, precision = 5, scale = 2)
    private BigDecimal examWork;

    /**
     * Total numeric result: always computed as termWork + examWork.
     * Server-derived; client value never trusted.
     * Range: 0–100.
     */
    @Column(name = "result", nullable = false, precision = 5, scale = 2)
    private BigDecimal result;

    /**
     * Letter grade derived from result using grade-mapping.json.
     * Server-derived; client value never trusted.
     * Examples: "A+", "A", "B", ..., "F"
     */
    @Column(name = "grade", nullable = false, length = 3)
    private String grade;

    /**
     * GPA points derived from result using grade-mapping.json.
     * Server-derived; client value never trusted.
     * Range: 0.0–4.0
     */
    @Column(name = "points", nullable = false, precision = 3, scale = 1)
    private BigDecimal points;

    /**
     * Whether the course is closed/finished.
     * Allows updating even after closing (product decision; can be constrained
     * later).
     * Default: false
     */
    @Column(name = "closed", nullable = false)
    private Boolean closed;

    /**
     * Timestamp of registration creation.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of last modification.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (closed == null) {
            closed = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}