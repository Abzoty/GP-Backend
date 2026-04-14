package com.gp.GP_backend.domain.space.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Read-only representation of a Space returned to the client. */
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

    /**
     * String name of the
     * {@link com.gp.GP_backend.domain.space.entity.SpaceCategory} enum value.
     */
    private String category;

    private String courseCode;
    private UUID createdById;
    private String createdByName;
    private Boolean isActive;
    private Integer memberCount;
    private LocalDateTime createdAt;

    /**
     * Jaccard similarity score relative to the candidate space submitted during
     * creation.
     * Populated only in the {@code 409 CONFLICT} similarity-check response;
     * omitted ({@code null}) from all other responses.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double similarityScore;
}