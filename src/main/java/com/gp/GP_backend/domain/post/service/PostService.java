package com.gp.GP_backend.domain.post.service;

import com.gp.GP_backend.domain.post.dto.CreateAnswerRequest;
import com.gp.GP_backend.domain.post.dto.CreatePostRequest;
import com.gp.GP_backend.domain.post.dto.EditAnswerRequest;
import com.gp.GP_backend.domain.post.dto.EditPostRequest;
import com.gp.GP_backend.domain.notification.service.NotificationService;
import com.gp.GP_backend.domain.post.dto.AllPostsResponse;
import com.gp.GP_backend.domain.post.dto.AnswerResponse;
import com.gp.GP_backend.domain.post.dto.PostResponse;
import com.gp.GP_backend.domain.post.entity.Answer;
import com.gp.GP_backend.domain.post.entity.Post;
import com.gp.GP_backend.domain.post.entity.TargetType;
import com.gp.GP_backend.domain.post.repository.AnswerRepository;
import com.gp.GP_backend.domain.post.repository.PostRepository;
import com.gp.GP_backend.domain.post.repository.VoteRepository;
import com.gp.GP_backend.domain.space.entity.Space;
import com.gp.GP_backend.domain.space.repository.SpaceMembershipRepository;
import com.gp.GP_backend.domain.space.repository.SpaceRepository;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.domain.user.service.GamificationService;
import com.gp.GP_backend.domain.user.service.UserService;
import com.gp.GP_backend.shared.exception.ApiException;
import com.gp.GP_backend.shared.util.XpCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

        private static final Set<String> POST_SORT_FIELDS = Set.of("goodQuestionCount", "createdAt");

        // ─── Dependencies ─────────────────────────────────────────────────────────
        private final PostRepository postRepository;
        private final AnswerRepository answerRepository;
        private final SpaceMembershipRepository spaceMembershipRepository;
        private final UserService userService;
        private final UserRepository userRepository;
        private final SpaceRepository spaceRepository;
        private final VoteRepository voteRepository;
        private final GamificationService gamificationService;
        private final NotificationService notificationService;

        // ─── Create a question post ───────────────────────────────────────

        @Transactional
        public PostResponse createPost(UUID spaceId, User author, CreatePostRequest request) {

                // Guard: author must be a space member
                boolean isMember = spaceMembershipRepository
                                .existsBySpaceIdAndUserId(spaceId, author.getId());
                if (!isMember) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "You must be a member of this space to post");
                }

                Space space = spaceRepository.findById(spaceId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                "Space not found with id: " + spaceId));

                Post post = Post.builder()
                                .space(space)
                                .author(author)
                                .title(request.getTitle().trim())
                                .body(request.getBody())
                                .createdAt(LocalDateTime.now())
                                .build();
                Post saved = postRepository.save(post);
                notificationService.notifyNewPostCreated(saved, author);
                log.debug("Post created: id={}, spaceId={}, authorId={}", saved.getId(), spaceId, author.getId());

                // Award XP to the author — same transaction
                gamificationService.awardXp(
                                author.getId(),
                                XpCalculator.EVENT_POST_CREATED,
                                XpCalculator.XP_POST_CREATED,
                                saved.getId(),
                                XpCalculator.REF_POST);

                return toPostResponse(saved, author.getFullName(), 0);
        }

        public UUID getSpaceIdForPost(UUID postId) {
                return postRepository.findSpaceIdByPostId(postId);
        }

        @Transactional
        public boolean editPost(EditPostRequest request, UUID userId, UUID postId) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                "Post not found with id: " + postId));
                if (!post.getAuthor().getId().equals(userId)) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "Only the post author can edit it");
                }
                post.setTitle(request.getTitle());
                post.setBody(request.getBody());
                postRepository.save(post);
                return true;
        }

        @Transactional
        public boolean deletePost(UUID postId, UUID userId) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                "Post not found with id: " + postId));
                if (!post.getAuthor().getId().equals(userId)) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "Only the post author can delete it");
                }

                gamificationService.revokeXp(
                                post.getAuthor().getId(),
                                XpCalculator.EVENT_POST_CREATED,
                                postId,
                                XpCalculator.REF_POST);

                // 1. Delete votes on all answers belonging to this post
                voteRepository.deleteVotesByPostAnswers(postId);

                // 2. Delete votes on the post itself (Good Question votes)
                voteRepository.deleteByTargetIdAndTargetType(postId, TargetType.QUESTION);

                // 3. Delete all answers
                answerRepository.deleteByPostId(postId);

                // 4. Delete the post
                postRepository.deleteById(postId);
                return true;
        }

        // ─── Answer a question ────────────────────────────────────────────

        @Transactional
        public AnswerResponse createAnswer(UUID postId, User author, CreateAnswerRequest request) {

                // Resolve the post — 404 if not found
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                "Post not found with id: " + postId));

                // Guard: author must be a member of the post's space
                boolean isMember = spaceMembershipRepository
                                .existsBySpaceIdAndUserId(post.getSpace().getId(), author.getId());
                if (!isMember) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "You must be a member of this space to answer");
                }

                Answer answer = Answer.builder()
                                .post(post)
                                .author(author)
                                .body(request.getBody())
                                .createdAt(LocalDateTime.now())
                                .build();

                Answer saved = answerRepository.save(answer);
                log.debug("Answer created: id={}, postId={}, authorId={}", saved.getId(), postId, author.getId());

                notificationService.notifyNewAnswer(post, saved, author);

                // Award XP to the answerer — same transaction
                gamificationService.awardXp(
                                author.getId(),
                                XpCalculator.EVENT_ANSWER_GIVEN,
                                XpCalculator.XP_ANSWER_GIVEN,
                                saved.getId(),
                                XpCalculator.REF_ANSWER);

                String authorName = author.getFullName();
                return toAnswerResponse(saved, authorName);
        }

        // ─── Mark question as solved ──────────────────────────────────────────────

        @Transactional
        public boolean markQuestionAsSolved(UUID postId, UUID answerId, User user) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                "Post not found with id: " + postId));
                if (!post.getAuthor().getId().equals(user.getId())) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "Only the question author can mark it as solved");
                }
                Answer answer = answerRepository.findById(answerId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                "Answer not found with id: " + answerId));
                if (!answer.getPost().getId().equals(postId)) {
                        throw new ApiException(HttpStatus.BAD_REQUEST,
                                        "Answer does not belong to the specified post");
                }
                UUID answerAuthorId = answer.getAuthor().getId();
                // Prevent self-accept farming: you cannot accept your own answer.
                if (answerAuthorId.equals(post.getAuthor().getId())) {
                        throw new ApiException(HttpStatus.BAD_REQUEST,
                                        "You cannot accept your own answer");
                }

                if (Boolean.TRUE.equals(post.getIsSolved())) {
                        UUID currentAcceptedId = post.getAcceptedAnswerId();
                        if (currentAcceptedId != null && currentAcceptedId.equals(answerId)) {
                                return true;
                        }

                        if (currentAcceptedId != null) {
                                Answer oldAccepted = answerRepository.findById(currentAcceptedId)
                                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                                "Accepted answer not found with id: "
                                                                                + currentAcceptedId));

                                answerRepository.unmarkAsAccepted(currentAcceptedId);
                                gamificationService.revokeXp(
                                                oldAccepted.getAuthor().getId(),
                                                XpCalculator.EVENT_ANSWER_ACCEPTED,
                                                currentAcceptedId,
                                                XpCalculator.REF_ANSWER);
                        }

                        if (!answer.getIsAccepted()) {
                                answerRepository.markAsAccepted(answerId);
                        }

                        postRepository.setAcceptedAnswer(postId, answerId);

                        gamificationService.awardXp(
                                        answerAuthorId,
                                        XpCalculator.EVENT_ANSWER_ACCEPTED,
                                        XpCalculator.XP_ANSWER_ACCEPTED,
                                        answerId,
                                        XpCalculator.REF_ANSWER);

                        return true;
                }

                // check the answer already accepted
                if (answer.getIsAccepted()) {
                        throw new ApiException(HttpStatus.BAD_REQUEST,
                                        "Answer is already accepted");
                }

                int acceptedRows = answerRepository.markAsAccepted(answerId);
                if (acceptedRows == 0) {
                        throw new ApiException(HttpStatus.BAD_REQUEST,
                                        "Answer is already accepted");
                }

                int solvedRows = postRepository.markAsSolved(postId, answerId);
                if (solvedRows == 0) {
                        // Roll back the acceptance as well (same transaction) to avoid partial state.
                        throw new ApiException(HttpStatus.BAD_REQUEST,
                                        "Post is already solved");
                }

                notificationService.notifyAnswerAccepted(answerId, post);

                // Award the answerer for having their answer accepted
                gamificationService.awardXp(
                                answerAuthorId,
                                XpCalculator.EVENT_ANSWER_ACCEPTED,
                                XpCalculator.XP_ANSWER_ACCEPTED,
                                answerId,
                                XpCalculator.REF_ANSWER);

                return true;
        }

        @Transactional
        public boolean unmarkQuestionAsSolved(UUID postId, User user) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                "Post not found with id: " + postId));
                if (!post.getAuthor().getId().equals(user.getId())) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "Only the question author can unmark it as solved");
                }

                if (!Boolean.TRUE.equals(post.getIsSolved())) {
                        return true;
                }

                UUID acceptedAnswerId = post.getAcceptedAnswerId();
                if (acceptedAnswerId != null) {
                        Answer accepted = answerRepository.findById(acceptedAnswerId)
                                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                        "Accepted answer not found with id: " + acceptedAnswerId));

                        answerRepository.unmarkAsAccepted(acceptedAnswerId);
                        gamificationService.revokeXp(
                                        accepted.getAuthor().getId(),
                                        XpCalculator.EVENT_ANSWER_ACCEPTED,
                                        acceptedAnswerId,
                                        XpCalculator.REF_ANSWER);
                }

                postRepository.clearSolved(postId);
                return true;
        }

        // ─── Read operations ──────────────────────────────────────────────────────

        @Transactional
        public PostResponse getPost(UUID postId) {
                Post post = postRepository.findById(postId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                "Post not found with id: " + postId));

                postRepository.incrementViewCount(postId);

                User author = userService.getUserById(post.getAuthor().getId());
                int answerCount = postRepository.countAnswersByPostId(postId);

                return toPostResponse(post, author.getFullName(), answerCount);
        }

        @Transactional
        public List<AnswerResponse> getPostAnswers(UUID postId, UUID userId, int page, int size) {

                if (!postRepository.existsById(postId)) {
                        throw new ApiException(HttpStatus.NOT_FOUND,
                                        "Post not found with id: " + postId);
                }

                if (!userRepository.existsById(userId)) {
                        throw new ApiException(HttpStatus.NOT_FOUND,
                                        "User not found with id: " + userId);
                }
                UUID spaceId = postRepository.findSpaceIdByPostId(postId);
                boolean isMember = spaceMembershipRepository
                                .existsBySpaceIdAndUserId(spaceId, userId);

                if (!isMember) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "You must be a member of this space to view answers");
                }

                Page<Answer> answers = answerRepository.findByPostIdOrderByIsAcceptedDescUpvoteCountDescCreatedAtAsc(
                                postId, PageRequest.of(page, size));

                Set<UUID> answerAuthorIds = answers.getContent().stream()
                                .map(answer -> answer.getAuthor().getId())
                                .collect(Collectors.toSet());
                Map<UUID, User> answerAuthors = userRepository.findAllById(answerAuthorIds).stream()
                                .collect(Collectors.toMap(User::getId, u -> u));

                return answers.stream()
                                .map(answer -> {
                                        User author = answerAuthors.get(answer.getAuthor().getId());
                                        if (author == null) {
                                                throw new ApiException(HttpStatus.NOT_FOUND,
                                                                "User not found with id: "
                                                                                + answer.getAuthor().getId());
                                        }
                                        return toAnswerResponse(answer, author.getFullName());
                                })
                                .toList();
        }

        @Transactional
        public boolean editAnswer(EditAnswerRequest request, UUID userId, UUID answerId) {
                Answer answer = answerRepository.findById(answerId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                "Answer not found with id: " + answerId));
                if (!answer.getAuthor().getId().equals(userId)) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "Only the answer author can edit it");
                }
                answer.setBody(request.getBody());
                answerRepository.save(answer);
                return true;
        }

        @Transactional
        public boolean deleteAnswer(UUID answerId, UUID userId) {
                Answer answer = answerRepository.findById(answerId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                                                "Answer not found with id: " + answerId));
                if (!answer.getAuthor().getId().equals(userId)) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "Only the answer author can delete it");
                }

                gamificationService.revokeXp(
                                answer.getAuthor().getId(),
                                XpCalculator.EVENT_ANSWER_GIVEN,
                                answerId,
                                XpCalculator.REF_ANSWER);

                // Delete all votes on this answer before removing it
                voteRepository.deleteByTargetIdAndTargetType(answerId, TargetType.ANSWER);

                answerRepository.deleteById(answerId);
                return true;
        }

        @Transactional(readOnly = true)
        public List<AllPostsResponse> getAllPost(UUID userId, UUID spaceId, int page, int size) {
                if (!spaceRepository.existsById(spaceId)) {
                        throw new ApiException(HttpStatus.NOT_FOUND, "Space not found with id: " + spaceId);
                }
                if (!userRepository.existsById(userId)) {
                        throw new ApiException(HttpStatus.NOT_FOUND, "User not found with id: " + userId);
                }
                if (!spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "You must be a member of this space to view posts");
                }

                Page<Post> postPage = postRepository.findBySpaceIdOrderByCreatedAtDesc(spaceId,
                                PageRequest.of(page, size));
                return mapToAllPostsResponses(postPage.getContent(), spaceId);
        }

        @Transactional(readOnly = true)
        public List<AllPostsResponse> searchPosts(
                        UUID userId, UUID spaceId, String query, Boolean isSolved,
                        String sortBy, String sortDir, int page, int size) {

                if (!spaceRepository.existsById(spaceId)) {
                        throw new ApiException(HttpStatus.NOT_FOUND, "Space not found with id: " + spaceId);
                }
                if (!spaceMembershipRepository.existsBySpaceIdAndUserId(spaceId, userId)) {
                        throw new ApiException(HttpStatus.FORBIDDEN,
                                        "You must be a member of this space to view posts");
                }

                Sort sort = buildSort(sortBy, sortDir);
                PageRequest pageable = PageRequest.of(page, size, sort);
                String normalizedQuery = (query == null || query.isBlank()) ? null : query.trim();

                Page<Post> postPage = postRepository.searchPosts(spaceId, normalizedQuery, isSolved, pageable);
                return mapToAllPostsResponses(postPage.getContent(), spaceId);
        }

        // ─── Mapping helpers ──────────────────────────────────────────────────────

        private PostResponse toPostResponse(Post post, String authorName, int answerCount) {

                return PostResponse.builder()
                                .id(post.getId())
                                .spaceId(post.getSpace().getId())
                                .authorId(post.getAuthor().getId())
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
                                .postId(answer.getPost().getId())
                                .authorId(answer.getAuthor().getId())
                                .authorName(authorName)
                                .body(answer.getBody())
                                .upvoteCount(answer.getUpvoteCount())
                                .isAccepted(answer.getIsAccepted())
                                .createdAt(answer.getCreatedAt())
                                .updatedAt(answer.getUpdatedAt())
                                .build();
        }

        private List<AllPostsResponse> mapToAllPostsResponses(List<Post> posts, UUID spaceId) {
                if (posts.isEmpty())
                        return List.of();

                // 1. Fetch the single space once
                Space space = spaceRepository.findById(spaceId)
                                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Space not found"));

                // 2. Batch fetch answer counts
                List<UUID> postIds = posts.stream().map(Post::getId).toList();
                List<Object[]> answerCountsRaw = answerRepository.countAnswersByPostIds(postIds);
                java.util.Map<UUID, Integer> answerCounts = new java.util.HashMap<>();
                for (Object[] row : answerCountsRaw) {
                        answerCounts.put((UUID) row[0], ((Number) row[1]).intValue());
                }

                // 3. Prepare to batch fetch users
                Set<UUID> userIdsToFetch = posts.stream().map(post -> post.getAuthor().getId())
                                .collect(java.util.stream.Collectors.toSet());

                // 4. Fetch top answers per post & collect their authors
                java.util.Map<UUID, List<Answer>> topAnswersMap = new java.util.HashMap<>();
                for (UUID pid : postIds) {
                        List<Answer> topAnswers = answerRepository
                                        .findByPostIdOrderByIsAcceptedDescUpvoteCountDescCreatedAtAsc(
                                                        pid, PageRequest.of(0, 3))
                                        .getContent();
                        topAnswersMap.put(pid, topAnswers);
                        topAnswers.forEach(a -> userIdsToFetch.add(a.getAuthor().getId()));
                }

                // 5. Batch fetch all Users (Post authors AND Answer authors at the same time)
                java.util.Map<UUID, User> usersMap = userRepository.findAllById(userIdsToFetch).stream()
                                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));

                // 6. Assemble the DTOs entirely in memory (Lightning fast)
                return posts.stream().map(post -> {
                        User author = usersMap.get(post.getAuthor().getId());
                        int count = answerCounts.getOrDefault(post.getId(), 0);
                        List<Answer> topAnswers = topAnswersMap.getOrDefault(post.getId(), List.of());

                        List<AllPostsResponse.AnswerSummary> answerSummaries = topAnswers.stream().map(ans -> {
                                User ansAuthor = usersMap.get(ans.getAuthor().getId());
                                return AllPostsResponse.AnswerSummary.builder()
                                                .answerId(ans.getId())
                                                .authorId(ans.getAuthor().getId())
                                                .authorName(ansAuthor != null ? ansAuthor.getFullName() : "Unknown")
                                                .authorAvatarUrl(ansAuthor != null ? ansAuthor.getImageUrl() : null)
                                                .body(ans.getBody())
                                                .upvoteCount(ans.getUpvoteCount())
                                                .isAccepted(ans.getIsAccepted())
                                                .createdAt(ans.getCreatedAt().toInstant(ZoneOffset.UTC))
                                                .build();
                        }).toList();

                        return AllPostsResponse.builder()
                                        .postId(post.getId())
                                        .title(post.getTitle())
                                        .body(post.getBody())
                                        .authorId(author != null ? author.getId() : post.getAuthor().getId())
                                        .authorName(author != null ? author.getFullName() : "Unknown")
                                        .authorAvatarUrl(author != null ? author.getImageUrl() : null)
                                        .spaceId(space.getId())
                                        .spaceName(space.getName())
                                        .goodQuestionCount(post.getGoodQuestionCount())
                                        .answerCount(count)
                                        .viewCount(post.getViewCount())
                                        .solved(post.getIsSolved())
                                        .top3Answers(answerSummaries)
                                        .createdAt(post.getCreatedAt().toInstant(ZoneOffset.UTC))
                                        .updatedAt(post.getUpdatedAt() != null
                                                        ? post.getUpdatedAt().toInstant(ZoneOffset.UTC)
                                                        : null)
                                        .build();
                }).toList();
        }

        private Sort buildSort(String sortBy, String sortDir) {
                Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                                ? Sort.Direction.ASC
                                : Sort.Direction.DESC;
                String field = (sortBy != null && POST_SORT_FIELDS.contains(sortBy)) ? sortBy : "createdAt";
                return Sort.by(direction, field);
        }

        private List<AllPostsResponse.AnswerSummary> buildTop3Answers(UUID postId) {
                List<Answer> topAnswers = answerRepository
                                .findByPostIdOrderByIsAcceptedDescUpvoteCountDescCreatedAtAsc(
                                                postId, PageRequest.of(0, 3))
                                .getContent();

                if (topAnswers.isEmpty()) {
                        return List.of();
                }

                Set<UUID> answerAuthorIds = topAnswers.stream()
                                .map(answer -> answer.getAuthor().getId())
                                .collect(Collectors.toCollection(HashSet::new));
                Map<UUID, User> answerAuthors = userRepository.findAllById(answerAuthorIds).stream()
                                .collect(Collectors.toMap(User::getId, u -> u));

                return topAnswers.stream()
                                .map(answer -> {
                                        User answerAuthor = answerAuthors.get(answer.getAuthor().getId());
                                        if (answerAuthor == null) {
                                                throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                                                                "Author not found for answer: " + answer.getId());
                                        }

                                        return AllPostsResponse.AnswerSummary.builder()
                                                        .answerId(answer.getId())
                                                        .authorId(answer.getAuthor().getId())
                                                        .authorName(answerAuthor.getFullName())
                                                        .authorAvatarUrl(answerAuthor.getImageUrl())
                                                        .body(answer.getBody())
                                                        .upvoteCount(answer.getUpvoteCount())
                                                        .isAccepted(answer.getIsAccepted())
                                                        .createdAt(answer.getCreatedAt().toInstant(ZoneOffset.UTC))
                                                        .build();
                                })
                                .toList();
        }

        private Map<UUID, Integer> mapAnswerCountsByPostId(List<UUID> postIds) {
                if (postIds.isEmpty()) {
                        return Collections.emptyMap();
                }

                Map<UUID, Integer> answerCounts = new HashMap<>();
                for (Object[] row : answerRepository.countByPostIds(postIds)) {
                        answerCounts.put((UUID) row[0], ((Long) row[1]).intValue());
                }
                return answerCounts;
        }
}