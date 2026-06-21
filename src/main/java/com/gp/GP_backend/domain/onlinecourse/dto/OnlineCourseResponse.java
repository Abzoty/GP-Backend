package com.gp.GP_backend.domain.onlinecourse.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OnlineCourseResponse {
    private String id;
    private String courseCode;
    private String courseName;
    private String source;
    private String title;
    private String url;
    private String description;
    private Double rating;
    private Integer reviews;
    private Double price;
    private Double score;
    private LocalDateTime lastUpdated;
}