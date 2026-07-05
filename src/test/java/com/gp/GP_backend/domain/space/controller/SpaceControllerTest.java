package com.gp.GP_backend.domain.space.controller;

import com.gp.GP_backend.domain.space.dto.MembershipResponse;
import com.gp.GP_backend.domain.space.dto.SpaceResponse;
import com.gp.GP_backend.domain.space.dto.UpdateSpaceRequest;
import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import com.gp.GP_backend.domain.space.service.SpaceService;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceControllerTest {

    @Mock
    private SpaceService spaceService;

    @InjectMocks
    private SpaceController controller;

    @Test
    void updateSpaceShouldReturnUpdatedSpaceEnvelope() {
        UUID spaceId = UUID.randomUUID();
        User user = user(UUID.randomUUID());
        UpdateSpaceRequest request = new UpdateSpaceRequest();
        request.setName("Advanced Algorithms");
        request.setDescription("Updated description");
        request.setCategory(SpaceCategory.COLLEGE_COURSE);
        request.setCourseCode("CS301");
        SpaceResponse response = SpaceResponse.builder()
                .id(spaceId)
                .name("Advanced Algorithms")
                .build();

        when(spaceService.updateSpace(spaceId, request, user)).thenReturn(response);

        ResponseEntity<ApiResponse<SpaceResponse>> entity = controller.updateSpace(spaceId, request, user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getData().getId()).isEqualTo(spaceId);
        assertThat(entity.getBody().getData().getName()).isEqualTo("Advanced Algorithms");

        verify(spaceService).updateSpace(spaceId, request, user);
    }

    @Test
    void grantAdminShouldReturnUpdatedMembershipEnvelope() {
        UUID spaceId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        User user = user(UUID.randomUUID());
        MembershipResponse response = MembershipResponse.builder()
                .spaceId(spaceId)
                .userId(memberId)
                .role("ADMIN")
                .build();

        when(spaceService.grantAdmin(spaceId, memberId, user)).thenReturn(response);

        ResponseEntity<ApiResponse<MembershipResponse>> entity = controller.grantAdmin(spaceId, memberId, user);

        assertThat(entity.getStatusCode().value()).isEqualTo(200);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getData().getSpaceId()).isEqualTo(spaceId);
        assertThat(entity.getBody().getData().getUserId()).isEqualTo(memberId);
        assertThat(entity.getBody().getData().getRole()).isEqualTo("ADMIN");

        verify(spaceService).grantAdmin(spaceId, memberId, user);
    }

    private static User user(UUID id) {
        return User.builder()
                .id(id)
                .email("student@example.com")
                .passwordHash("hash")
                .fullName("Student Name")
                .build();
    }
}