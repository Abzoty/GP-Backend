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
 * Represents a user's course registration with optional derived grade, result,
 * and
 * points.
 *
 * Key differences from the old CourseRegistered:
 * - ID is Long (auto-increment IDENTITY), not UUID
 * - No courseCode/courseName client input; only course code stored
 * - No semester/academicYear fields (redundant with user's current_semester)
 * - grade, result, points are server-derived when both termWork and examWork
 * are provided
 * - Can be registered with null grades (pending state) and updated later
 * - unique constraint is (user_id, code) — same course only once
 * - termWork (0–40) and examWork (0–60) together compute result = termWork +
 * examWork (nullable)
 * - closed flag indicates course is finished/graded
 *
 * Nullable grade fields behavior:
 * - If termWork and examWork are both NULL: result, grade, points remain NULL
 * (pending grades)
 * - If termWork and examWork are both provided: result, grade, points are
 * calculated
 * - result = termWork + examWork (range 0–100)
 * - grade and points derived from result via grade-mapping.json
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
     * Nullable: can be null if grades are not yet provided (pending).
     */
    @Column(name = "term_work", nullable = true, precision = 5, scale = 2)
    private BigDecimal termWork;

    /**
     * Exam work score (final exam), typically 0–60.
     * Client supplies this; server never infers it.
     * Nullable: can be null if grades are not yet provided (pending).
     */
    @Column(name = "exam_work", nullable = true, precision = 5, scale = 2)
    private BigDecimal examWork;

    /**
     * Total numeric result: computed as termWork + examWork when both are provided.
     * Server-derived; client value never trusted.
     * Range: 0–100, or NULL if grades are pending.
     */
    @Column(name = "result", nullable = true, precision = 5, scale = 2)
    private BigDecimal result;

    /**
     * Letter grade derived from result using grade-mapping.json.
     * Server-derived; client value never trusted.
     * Examples: "A+", "A", "B", ..., "F", or NULL if grades are pending.
     */
    @Column(name = "grade", nullable = true, length = 3)
    private String grade;

    /**
     * GPA points derived from result using grade-mapping.json.
     * Server-derived; client value never trusted.
     * Range: 0.0–4.0, or NULL if grades are pending.
     */
    @Column(name = "points", nullable = true, precision = 3, scale = 1)
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