package com.gp.GP_backend.domain.onlinecourse.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "online_courses", indexes = {
    @Index(name = "idx_online_courses_code", columnList = "course_code")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnlineCourse {

    @Id
    @Column(length = 500)
    private String id;

    @Column(name = "course_code", nullable = false, length = 30)
    private String courseCode;

    @Column(name = "course_name", nullable = false, length = 300)
    private String courseName;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 1024)
    private String url;

    @Column(length = 2000)
    private String description;

    private Double rating;
    private Integer reviews;
    private Double price;
    private Double score;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}