package com.gp.GP_backend.domain.post.repository;

import com.gp.GP_backend.domain.post.entity.Answer;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    /**
     * Returns answers for a post sorted by:
     * 1. Accepted answer first (pinned at top — US-018)
     * 2. Highest upvote count
     * 3. Oldest first (stable ordering for equal scores)
     *
     * "PostId" resolves to the nested path post.id even though {@code post}
     * is now a {@code @ManyToOne} association.
     */
    Page<Answer> findByPostIdOrderByIsAcceptedDescUpvoteCountDescCreatedAtAsc(
            UUID postId, Pageable pageable);

    /** Used to increment the denormalised counter (US-016 upvote flow). */
    @Modifying
    @Query("UPDATE Answer a SET a.upvoteCount = a.upvoteCount + 1 WHERE a.id = :id")
    void incrementUpvoteCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Answer a SET a.upvoteCount = CASE WHEN a.upvoteCount > 0 THEN a.upvoteCount - 1 ELSE 0 END WHERE a.id = :id")
    void decrementUpvoteCount(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE Answer a SET a.isAccepted = true WHERE a.id = :id AND a.isAccepted = false")
    int markAsAccepted(@Param("id") UUID answerId);

    @Modifying
    @Query("UPDATE Answer a SET a.isAccepted = false WHERE a.id = :id AND a.isAccepted = true")
    int unmarkAsAccepted(@Param("id") UUID answerId);

    int countByPostId(UUID postId);

    @Query("SELECT COUNT(a) FROM Answer a WHERE a.post.id = :postId")
    int getAnswerCountByPostId(UUID postId);

    @Query("SELECT a.post.id, COUNT(a) FROM Answer a WHERE a.post.id IN :postIds GROUP BY a.post.id")
    List<Object[]> countByPostIds(@Param("postIds") List<UUID> postIds);

    /** Now a direct association traversal — no explicit cross-join needed. */
    @Query("SELECT a.author.fullName FROM Answer a WHERE a.id = :answerId")
    String findAuthorNameByAnswerId(@Param("answerId") UUID answerId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Answer a WHERE a.post.id = :postId")
    void deleteByPostId(UUID postId);

    /**
     * Counts answers submitted by a specific user in a specific space
     * (answers whose parent post belongs to that space).
     * Used by the space leaderboard to compute per-space answer stats.
     */
    @Query("""
            SELECT COUNT(a) FROM Answer a
            WHERE a.author.id = :userId
            AND a.post.space.id = :spaceId
            """)
    int countByAuthorIdInSpace(@Param("userId") UUID userId, @Param("spaceId") UUID spaceId);

    @Query("""
            SELECT a.author.id, COUNT(a)
            FROM Answer a
            WHERE a.post.space.id = :spaceId
            GROUP BY a.author.id
            """)
    List<Object[]> countBySpaceIdGroupByAuthor(@Param("spaceId") UUID spaceId);

    // Fetches answer counts for multiple posts in ONE query
    @Query("SELECT a.post.id, COUNT(a) FROM Answer a WHERE a.post.id IN :postIds GROUP BY a.post.id")
    java.util.List<Object[]> countAnswersByPostIds(@Param("postIds") java.util.List<UUID> postIds);
}