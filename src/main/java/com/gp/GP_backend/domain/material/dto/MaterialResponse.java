package com.gp.GP_backend.domain.material.dto;

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
public class MaterialResponse {

    private UUID id;
    private UUID spaceId;
    private String spaceName;
    private UUID uploadedById;
    private String uploadedByName;
    private String title;
    private String description;

    // from enum ResourceType
    private String resourceType;
    
    private String url;

    /** File size in kilobytes; null for link materials. */
    private Integer fileSizeKb;

    private Integer linkCount;
    private Boolean isBookmarked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}