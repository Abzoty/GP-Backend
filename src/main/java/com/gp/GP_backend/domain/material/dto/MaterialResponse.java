package com.gp.GP_backend.domain.material.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class MaterialResponse {
    private Long id;
    private Long spaceId;
    private String spaceName;
    private Long uploadedById;
    private String uploadedByName;
    private String title;
    private String description;
    private String resourceType;
    private String url;
    private Integer fileSizeKb;
    private Integer linkCount;
    private LocalDateTime createdAt;
}
