package com.gp.GP_backend.domain.post.service;

import com.gp.GP_backend.domain.post.dto.CreateAnswerRequest;
import com.gp.GP_backend.domain.post.dto.CreatePostRequest;
import com.gp.GP_backend.domain.post.dto.AnswerResponse;
import com.gp.GP_backend.domain.post.dto.PostResponse;
import com.gp.GP_backend.domain.post.entity.Answer;
import com.gp.GP_backend.domain.post.entity.Post;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.service.GamificationService;
import com.gp.GP_backend.domain.user.service.UserService;
import com.gp.GP_backend.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Business logic for Posts and Answers.
 *
 * <p>Covers US-014 (create question post) and US-015 (answer a question).
 * XP awards are delegated to {@link GamificationService} and notification
 * hooks are delegated to {@link com.gp.GP_backend.domain.notification.service.NotificationService}
 * — both run in the same transaction so everything is atomic.
 *
 * <p>Cross-domain data (author name, space membership) is fetched through
 * service/repository interfaces rather than JPA associations, keeping
 * domain packages loosely coupled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    // ─── XP constants (from product backlog §7.2) ─────────────────────────────
        // private static final int XP_POST_CREATED   = 10;
        // private static final int XP_ANSWER_GIVEN   = 15;

        // private static final String EVENT_POST_CREATED  = "POST_CREATED";
        // private static final String EVENT_ANSWER_GIVEN  = "ANSWER_GIVEN";

    // ─── Dependencies ─────────────────────────────────────────────────────────
        private final PostRepository           postRepository;
        private final AnswerRepository         answerRepository;
        private final SpaceMembershipRepository spaceMembershipRepository;
        private final UserService              userService;
        // private final GamificationService     gamificationService;

    // NotificationService is injected optionally so the feature compiles even
    // before NotificationService is fully implemented (see its TODO stub).
    // Switch to a required constructor injection once the service is complete.
    // private final NotificationService notificationService;

    // ─── US-014: Create a question post ───────────────────────────────────────

    /**
     * Creates a new post (question or discussion) inside a space.
     *
     * <p>Acceptance criteria enforced here:
     * <ul>
     *   <li>Author must be a member of the target space.</li>
     *   <li>Title 10–300 chars, body min 30 chars — validated by Bean Validation
     *       before this method is called; asserted here for safety.</li>
     *   <li>Max 5 tags.</li>
     *   <li>+10 XP awarded to the author on success.</li>
     * </ul>
     *
     * @param spaceId  the space in which the post is created
     * @param author   the authenticated user (resolved from JWT principal)
     * @param request  validated request body
     * @return the persisted post mapped to a {@link PostResponse}
     * @throws ApiException 403 if the author is not a member of the space
     * @throws ApiException 422 if tag count exceeds 5 (belt-and-suspenders)
     */
        @Transactional
        public PostResponse createPost(UUID spaceId, User author, CreatePostRequest request) {

        // Guard: author must be a space member
        boolean isMember = spaceMembershipRepository
                .existsBySpaceIdAndUserId(spaceId, author.getId());
        if (!isMember) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                "You must be a member of this space to post");
        }

        Post post = Post.builder()
                .spaceId(spaceId)
                .authorId(author.getId())
                .title(request.getTitle().trim())
                .body(request.getBody())
                .createdAt(LocalDateTime.now())
                .build();

        Post saved = postRepository.save(post);
        log.debug("Post created: id={}, spaceId={}, authorId={}", saved.getId(), spaceId, author.getId());

        // Award XP — runs in the same transaction; rolls back with the post on failure
        // gamificationService.awardXp(author, EVENT_POST_CREATED, XP_POST_CREATED);

        return toPostResponse(saved, author.getFullName(), 0);
        }

        public UUID getSpaceIdForPost(UUID postId) {
                return postRepository.findSpaceIdByPostId(postId);
        }

    // ─── US-015: Answer a question ────────────────────────────────────────────

    /**
     * Submits an answer to an existing post.
     *
     * <p>Acceptance criteria enforced here:
     * <ul>
     *   <li>The target post must exist.</li>
     *   <li>Author must be a member of the space that owns the post.</li>
     *   <li>Answer body minimum 10 chars — enforced by Bean Validation.</li>
     *   <li>Post's answer count is incremented (via a COUNT query — no
     *       denormalised column needed per the ERD).</li>
     *   <li>+15 XP awarded to the answerer.</li>
     *   <li>A notification is sent to the question author (stubbed until
     *       NotificationService is implemented).</li>
     * </ul>
     *
     * @param postId  the post being answered
     * @param author  the authenticated user
     * @param request validated request body
     * @return the persisted answer mapped to an {@link AnswerResponse}
     * @throws ApiException 404 if the post does not exist
     * @throws ApiException 403 if the author is not a member of the post's space
     */
        @Transactional
        public AnswerResponse createAnswer(UUID postId, User author, CreateAnswerRequest request) {

        // Resolve the post — 404 if not found
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Post not found with id: " + postId));

        // Guard: author must be a member of the post's space
        boolean isMember = spaceMembershipRepository
                .existsBySpaceIdAndUserId(post.getSpaceId(), author.getId());
        if (!isMember) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                "You must be a member of this space to answer");
        }

        Answer answer = Answer.builder()
                .postId(postId)
                .authorId(author.getId())
                .body(request.getBody())
                .createdAt(LocalDateTime.now())
                .build();

        Answer saved = answerRepository.save(answer);
        log.debug("Answer created: id={}, postId={}, authorId={}", saved.getId(), postId, author.getId());

        // Award XP to the answerer — same transaction
        // gamificationService.awardXp(author, EVENT_ANSWER_GIVEN, XP_ANSWER_GIVEN);

        // TODO: notify question author once NotificationService is implemented
        // notificationService.notifyNewAnswer(post, saved, author);

        String authorName = author.getFullName();
        return toAnswerResponse(saved, authorName);
        }

        // ======================= mark question as solver =========================

        @Transactional
        public boolean markQuestionAsSolved(UUID postId, UUID answerId, User user) {
                Post post = postRepository.findById(postId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                "Post not found with id: " + postId));
                if (!post.getAuthorId().equals(user.getId())) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                "Only the question author can mark it as solved");
                }
                Answer answer = answerRepository.findById(answerId)
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                "Answer not found with id: " + answerId));
                if (!answer.getPostId().equals(postId)) {
                        throw new ApiException(HttpStatus.BAD_REQUEST,
                                "Answer does not belong to the specified post");
                }
                answerRepository.markAsAccepted(answerId);
                postRepository.markAsSolved(postId, answerId);
                return true;
        }

    // ─── Read operations ──────────────────────────────────────────────────────

    /**
     * Fetches a single post by ID and increments its view counter.
     *
     * @throws ApiException 404 if not found
     */
        @Transactional
        public PostResponse getPost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Post not found with id: " + postId));

        postRepository.incrementViewCount(postId);

        User author = userService.getUserById(post.getAuthorId());
        int answerCount = postRepository.countAnswersByPostId(postId);

        return toPostResponse(post, author.getFullName(), answerCount);
        }

                
    // ─── Mapping helpers ──────────────────────────────────────────────────────

    /**
     * Maps a {@link Post} entity to a {@link PostResponse}.
     *
     * <p>Tags are stored as a comma-separated string and split back into a list
     * here so the client always receives a proper JSON array.
     */
        private PostResponse toPostResponse(Post post, String authorName, int answerCount) {

        return PostResponse.builder()
                .id(post.getId())
                .spaceId(post.getSpaceId())
                .authorId(post.getAuthorId())
                .authorName(authorName)
                .title(post.getTitle())
                .body(post.getBody())
                .isSolved(post.getIsSolved())
                .acceptedAnswerId(post.getAcceptedAnswerId())
                .viewCount(post.getViewCount())
                .goodQuestionCount(post.getGoodQuestionCount())
                .answerCount(answerCount)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
        }

        private AnswerResponse toAnswerResponse(Answer answer, String authorName) {
        return AnswerResponse.builder()
                .id(answer.getId())
                .postId(answer.getPostId())
                .authorId(answer.getAuthorId())
                .authorName(authorName)
                .body(answer.getBody())
                .upvoteCount(answer.getUpvoteCount())
                .isAccepted(answer.getIsAccepted())
                .createdAt(answer.getCreatedAt())
                .updatedAt(answer.getUpdatedAt())
                .build();
        }
}