package com.gp.GP_backend.domain.space.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class SpaceResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String category;
    private String courseCode;
    private Long createdById;
    private String createdByName;
    private Boolean isActive;
    private Integer memberCount;
    private LocalDateTime createdAt;
}
