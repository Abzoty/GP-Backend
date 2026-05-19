package com.gp.GP_backend.domain.space.service;

import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private SpaceMembershipRepository membershipRepository;

    @InjectMocks
    private SpaceService spaceService;

    @Test
    void getSpaceByIdShouldThrowForbiddenForNonMember() {
        UUID spaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(membershipRepository.findBySpaceIdAndUserIdWithSpaceAndCreator(spaceId, userId))
            .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> spaceService.getSpaceById(spaceId, userId));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("You are not a member of this space", ex.getMessage());
    }

    @Test
    void joinSpaceShouldMapMembershipRaceToConflict() {
        UUID spaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = User.builder().id(userId).build();
        Space space = Space.builder().id(spaceId).memberCount(1).build();

        when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
        when(membershipRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        ApiException ex = assertThrows(ApiException.class, () -> spaceService.joinSpace(spaceId, user));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("You are already a member of this space", ex.getMessage());
    }
}
