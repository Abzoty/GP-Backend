package com.gp.GP_backend.integration;

import com.gp.GP_backend.domain.material.dto.ShareLinkRequest;
import com.gp.GP_backend.domain.material.entity.Material;
import com.gp.GP_backend.domain.material.repository.MaterialRepository;
import com.gp.GP_backend.domain.material.service.MaterialService;
import com.gp.GP_backend.domain.post.dto.CreateAnswerRequest;
import com.gp.GP_backend.domain.post.dto.CreatePostRequest;
import com.gp.GP_backend.domain.post.entity.Answer;
import com.gp.GP_backend.domain.post.entity.Post;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.post.service.PostService;
import com.gp.GP_backend.domain.space.dto.CreateSpaceRequest;
import com.gp.GP_backend.domain.space.dto.SpaceResponse;
import com.gp.GP_backend.domain.space.entity.SpaceCategory;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.service.SpaceService;
import com.gp.GP_backend.domain.user.entity.GamificationProfile;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.GamificationProfileRepository;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.shared.util.XpCalculator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class CompleteCycleIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private SpaceMembershipRepository spaceMembershipRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private GamificationProfileRepository gamificationProfileRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void completeCycle_spacePostAnswerSolved_awardsXpAndUpdatesCounters() {
        User questionAuthor = userRepository.save(newUser("author@gp.com", "Author"));
        User answerAuthor = userRepository.save(newUser("answerer@gp.com", "Answerer"));

        UUID spaceId = createSpace(questionAuthor, "Integration Space");
        spaceService.joinSpace(spaceId, answerAuthor);

        Post createdPost = createPost(spaceId, questionAuthor);
        Answer createdAnswer = createAnswer(createdPost.getId(), answerAuthor);

        boolean solved = postService.markQuestionAsSolved(createdPost.getId(), createdAnswer.getId(), questionAuthor);
        assertTrue(solved);

        // markAsAccepted/markAsSolved are bulk updates; clear persistence context to avoid stale reads.
        entityManager.flush();
        entityManager.clear();

        GamificationProfile authorProfile = gamificationProfileRepository.findByUserId(questionAuthor.getId()).orElseThrow();
        GamificationProfile answererProfile = gamificationProfileRepository.findByUserId(answerAuthor.getId()).orElseThrow();

        assertEquals(1, authorProfile.getTotalPosts());
        assertEquals(XpCalculator.XP_POST_CREATED, authorProfile.getXpPoints());

        assertEquals(1, answererProfile.getTotalAnswers());
        assertEquals(XpCalculator.XP_ANSWER_GIVEN + XpCalculator.XP_ANSWER_ACCEPTED, answererProfile.getXpPoints());

        Post reloadedPost = postRepository.findById(createdPost.getId()).orElseThrow();
        assertTrue(reloadedPost.getIsSolved());
        assertEquals(createdAnswer.getId(), reloadedPost.getAcceptedAnswerId());

        Answer reloadedAnswer = answerRepository.findById(createdAnswer.getId()).orElseThrow();
        assertTrue(reloadedAnswer.getIsAccepted());
    }

    @Test
    @Transactional
    void completeCycle_materialShareAndBookmark_awardsXpToOwnerAndUpdatesLinkCount() {
        User spaceOwner = userRepository.save(newUser("owner@gp.com", "Owner"));
        User otherUser = userRepository.save(newUser("other@gp.com", "Other"));

        UUID spaceId = createSpace(spaceOwner, "Materials Space");
        spaceService.joinSpace(spaceId, otherUser);

        ShareLinkRequest shareLinkRequest = new ShareLinkRequest();
        shareLinkRequest.setSpaceId(spaceId);
        shareLinkRequest.setTitle("Useful link");
        shareLinkRequest.setDescription("desc");
        shareLinkRequest.setUrl("https://example.com");

        UUID materialId = materialService.shareLink(shareLinkRequest, spaceOwner).getId();
        assertNotNull(materialId);

        materialService.bookmark(materialId, otherUser);

        // linkCount is updated via bulk UPDATE; clear persistence context to avoid stale reads.
        entityManager.flush();
        entityManager.clear();

        Material reloaded = materialRepository.findById(materialId).orElseThrow();
        assertEquals(1, reloaded.getLinkCount());

        GamificationProfile ownerProfile = gamificationProfileRepository.findByUserId(spaceOwner.getId()).orElseThrow();
        assertEquals(XpCalculator.XP_MATERIAL_SHARED + XpCalculator.XP_MATERIAL_LINKED, ownerProfile.getXpPoints());
        assertEquals(1, ownerProfile.getTotalMaterialsShared());
    }

    private UUID createSpace(User creator, String name) {
        CreateSpaceRequest request = new CreateSpaceRequest();
        request.setName(name);
        request.setDescription("desc");
        request.setCategory(SpaceCategory.TUTORIAL);

        SpaceResponse created = spaceService.createSpace(request, creator);
        assertNotNull(created.getId());

        // sanity: creator is member
        assertTrue(spaceMembershipRepository.existsBySpaceIdAndUserId(created.getId(), creator.getId()));
        return created.getId();
    }

    private Post createPost(UUID spaceId, User author) {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("A valid post title");
        request.setBody("This body is long enough to satisfy validation rules.");

        UUID postId = postService.createPost(spaceId, author, request).getId();
        assertNotNull(postId);

        return postRepository.findById(postId).orElseThrow();
    }

    private Answer createAnswer(UUID postId, User author) {
        CreateAnswerRequest request = new CreateAnswerRequest();
        request.setBody("This answer is long enough.");

        UUID answerId = postService.createAnswer(postId, author, request).getId();
        assertNotNull(answerId);

        return answerRepository.findById(answerId).orElseThrow();
    }

    private static User newUser(String email, String fullName) {
        return User.builder()
                .email(email)
                .passwordHash("hash")
                .fullName(fullName)
                .studentId("SID-" + UUID.randomUUID())
                .isActive(true)
                .build();
    }
}
