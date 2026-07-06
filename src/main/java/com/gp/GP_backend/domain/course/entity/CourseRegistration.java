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

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "term_work", nullable = true, precision = 5, scale = 2)
    private BigDecimal termWork;

    @Column(name = "exam_work", nullable = true, precision = 5, scale = 2)
    private BigDecimal examWork;

    @Column(name = "result", nullable = true, precision = 5, scale = 2)
    private BigDecimal result;

    @Column(name = "grade", nullable = true, length = 3)
    private String grade;

    @Column(name = "points", nullable = true, precision = 3, scale = 1)
    private BigDecimal points;

    @Column(name = "closed", nullable = false)
    private Boolean closed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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