package com.gp.GP_backend.domain.space.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpaceResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String category;
    private String courseCode;
    private UUID createdById;
    private String createdByName;
    private Boolean isActive;
    private Integer memberCount;
    private LocalDateTime createdAt;


    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double similarityScore;

    private String role;
}