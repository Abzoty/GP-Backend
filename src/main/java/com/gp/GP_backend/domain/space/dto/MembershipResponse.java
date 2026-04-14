package com.gp.GP_backend.domain.space.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only snapshot of a single {@code space_memberships} row returned to the
 * client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipResponse {

    private UUID id;
    private UUID spaceId;
    private String spaceName;
    private UUID userId;
    private String userName;

    /** MEMBER / MODERATOR / OWNER / ADMIN */
    private String role;

    private LocalDateTime joinedAt;
}