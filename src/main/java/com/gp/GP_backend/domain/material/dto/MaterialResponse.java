package com.gp.GP_backend.domain.material.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only representation of a
 * {@link com.gp.GP_backend.domain.material.entity.Material}
 * returned to the client.
 *
 * <p>
 * {@code isBookmarked} reflects whether the <em>requesting user</em> has
 * bookmarked this material. It is populated by the service on every read
 * so the frontend never needs a second round-trip.
 *
 * <p>
 * {@code linkCount} is the total number of users who have bookmarked this
 * material — useful for surfacing "most popular" content in a space.
 */
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

    /**
     * {@link com.gp.GP_backend.domain.material.entity.AcceptedFileType} name
     * (e.g. {@code "PDF"}) for uploaded files, or {@code "LINK"} for external URLs.
     */
    private String resourceType;

    /**
     * Stored filename (UUID + extension) for files, or the full external URL
     * for links.
     */
    private String url;

    /** File size in kilobytes; {@code null} for link materials. */
    private Integer fileSizeKb;

    /** Total number of users who have bookmarked this material. */
    private Integer linkCount;

    /**
     * {@code true} if the authenticated user who made the request has bookmarked
     * this.
     */
    private Boolean isBookmarked;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}