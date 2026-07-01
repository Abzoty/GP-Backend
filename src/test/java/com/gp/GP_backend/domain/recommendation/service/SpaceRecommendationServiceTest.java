package com.gp.GP_backend.domain.recommendation.service;

import com.gp.GP_backend.domain.course.entity.CourseRegistration;
import com.gp.GP_backend.domain.course.repository.CourseRegistrationRepository;
import com.gp.GP_backend.domain.recommendation.client.RecommendationClient;
import com.gp.GP_backend.domain.recommendation.dto.SpaceRankingResponse;
import com.gp.GP_backend.domain.recommendation.dto.SpaceRecommendationRankRequest;
import com.gp.GP_backend.domain.recommendation.dto.SpaceRecommendationResponse;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import com.gp.GP_backend.domain.space.entity.SpaceMembership;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpaceRecommendationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRegistrationRepository courseRegistrationRepository;

    @Mock
    private SpaceMembershipRepository membershipRepository;

    @Mock
    private SpaceRepository spaceRepository;

    @Mock
    private RecommendationClient recommendationClient;

    @InjectMocks
    private SpaceRecommendationService service;

    @Test
    void recommendShouldReturnEmptyListWhenNoCandidatesExist() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(courseRegistrationRepository.findByUserIdAndClosedFalse(userId)).thenReturn(List.of());
        when(membershipRepository.findByUserIdWithSpace(userId)).thenReturn(List.of());
        when(membershipRepository.findFofSpaceScores(userId)).thenReturn(List.of());
        when(spaceRepository.findPopularSpacesNotJoined(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<SpaceRecommendationResponse> responses = service.recommend(userId, 20);

        assertThat(responses).isEmpty();
        verifyNoInteractions(recommendationClient);
        verify(spaceRepository, never()).findAllByIdWithCreator(anyCollection());
    }

    @Test
    void recommendShouldMergeSourcesAndFilterUnknownRanks() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);

        Space joinedSpace = space(UUID.randomUUID(), "Joined Space", 10, LocalDateTime.now().minusDays(5));
        Space multiSourceSpace = space(UUID.randomUUID(), "Alpha Space", 25, LocalDateTime.now().minusDays(2));
        Space socialOnlySpace = space(UUID.randomUUID(), "Beta Space", 20, LocalDateTime.now().minusDays(3));
        Space popularOnlySpace = space(UUID.randomUUID(), "Gamma Space", 15, LocalDateTime.now().minusDays(1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(courseRegistrationRepository.findByUserIdAndClosedFalse(userId)).thenReturn(List.of(
                courseRegistration(" cs101 "),
                courseRegistration("CS101"),
                courseRegistration(null)));
        when(membershipRepository.findByUserIdWithSpace(userId))
                .thenReturn(List.of(SpaceMembership.builder().space(joinedSpace).user(user).build()));
        when(spaceRepository.findCollegeCourseSpacesNotJoined(List.of("CS101"), userId, SpaceCategory.COLLEGE_COURSE))
                .thenReturn(List.of(multiSourceSpace));
        when(membershipRepository.findFofSpaceScores(userId))
                .thenReturn(List.of(
                        new Object[] { multiSourceSpace.getId(), 7L },
                        new Object[] { socialOnlySpace.getId(), 5L }));
        when(spaceRepository.findPopularSpacesNotJoined(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(multiSourceSpace, popularOnlySpace)));
        when(spaceRepository.findAllByIdWithCreator(anyCollection()))
                .thenReturn(List.of(multiSourceSpace, socialOnlySpace, popularOnlySpace));
        when(recommendationClient.rankSpaces(any()))
                .thenReturn(List.of(
                        SpaceRankingResponse.builder().spaceId(socialOnlySpace.getId()).score(0.92).build(),
                        SpaceRankingResponse.builder().spaceId(multiSourceSpace.getId()).score(0.88).build(),
                        SpaceRankingResponse.builder().spaceId(popularOnlySpace.getId()).score(0.73).build(),
                        SpaceRankingResponse.builder().spaceId(UUID.randomUUID()).score(0.99).build()));

        List<SpaceRecommendationResponse> responses = service.recommend(userId, 2);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getSpace().getId()).isEqualTo(socialOnlySpace.getId());
        assertThat(responses.get(0).getScore()).isEqualTo(0.92);
        assertThat(responses.get(0).getMethodCount()).isEqualTo(1);
        assertThat(responses.get(0).getReasons()).containsExactly("SOCIAL");

        assertThat(responses.get(1).getSpace().getId()).isEqualTo(multiSourceSpace.getId());
        assertThat(responses.get(1).getScore()).isEqualTo(0.88);
        assertThat(responses.get(1).getMethodCount()).isEqualTo(3);
        assertThat(responses.get(1).getReasons())
                .containsExactlyInAnyOrder("COURSE_MATCH", "SOCIAL", "POPULAR");

        ArgumentCaptor<SpaceRecommendationRankRequest> requestCaptor = ArgumentCaptor.forClass(SpaceRecommendationRankRequest.class);
        verify(recommendationClient).rankSpaces(requestCaptor.capture());
        SpaceRecommendationRankRequest request = requestCaptor.getValue();
        assertThat(request.getUser().getId()).isEqualTo(userId);
        assertThat(request.getUser().getCourses()).containsExactly("CS101");
        assertThat(request.getUser().getJoinedSpaces()).hasSize(1);
        assertThat(request.getCandidateSpaces()).extracting(SpaceRecommendationRankRequest.CandidateSpace::getId)
                .containsExactly(multiSourceSpace.getId(), socialOnlySpace.getId(), popularOnlySpace.getId());
    }

    private static User user(UUID userId) {
        return User.builder()
                .id(userId)
                .email("student@example.com")
                .passwordHash("hash")
                .fullName("Student Name")
                .build();
    }

    private static CourseRegistration courseRegistration(String code) {
        return CourseRegistration.builder()
                .code(code)
                .build();
    }

    private static Space space(UUID id, String name, int memberCount, LocalDateTime createdAt) {
        return Space.builder()
                .id(id)
                .name(name)
                .slug(name.toLowerCase().replace(' ', '-'))
                .description(name + " description")
                .category(SpaceCategory.COLLEGE_COURSE)
                .courseCode("CS101")
                .createdBy(user(UUID.randomUUID()))
                .isActive(true)
                .memberCount(memberCount)
                .createdAt(createdAt)
                .build();
    }
}