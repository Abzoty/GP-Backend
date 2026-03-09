package com.gp.GP_backend.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "courses_registered")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CourseRegistered {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "course_code", nullable = false, length = 30)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    private Short semester;

    @Column(name = "academic_year")
    private Short academicYear;

    @Column(length = 5)
    private String grade;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = true;
}
