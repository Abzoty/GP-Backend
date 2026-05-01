package com.gp.GP_backend.domain.post.repository;

import com.gp.GP_backend.domain.post.entity.Answer;

import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AnswerRepository extends JpaRepository<Answer, UUID> {

        /**
         * Returns answers for a post sorted by:
         * 1. Accepted answer first (pinned at top — US-018)
         * 2. Highest upvote count
         * 3. Oldest first (stable ordering for equal scores)
         */
        Page<Answer> findByPostIdOrderByIsAcceptedDescUpvoteCountDescCreatedAtAsc(
                        UUID postId, Pageable pageable);

        /** Used to increment the denormalised counter (US-016 upvote flow). */
        @Modifying
        @Query("UPDATE Answer a SET a.upvoteCount = a.upvoteCount + 1 WHERE a.id = :id")
        void incrementUpvoteCount(@Param("id") UUID id);

        @Modifying
        @Query("UPDATE Answer a SET a.isAccepted = true WHERE a.id = :id")
        void markAsAccepted(@Param("id") UUID answerId);

        int countByPostId(UUID postId);

        @Query("SELECT COUNT(a) FROM Answer a WHERE a.postId = :postId")
        int getAnswerCountByPostId(UUID postId);

        @Query("SELECT u.fullName FROM Answer a, User u WHERE a.id = :answerId AND a.authorId = u.id")
        String findAuthorNameByAnswerId(UUID answerId);

        @Modifying
        @Transactional
        @Query("DELETE FROM Answer a WHERE a.postId = :postId")
        void deleteByPostId(UUID postId);

        /**
         * Counts answers submitted by a specific user in a specific space
         * (answers whose parent post belongs to that space).
         * Used by the space leaderboard to compute per-space answer stats.
         */
        @Query("""
                        SELECT COUNT(a) FROM Answer a
                        WHERE a.authorId = :userId
                        AND a.postId IN (SELECT p.id FROM Post p WHERE p.spaceId = :spaceId)
                        """)
        int countByAuthorIdInSpace(@Param("userId") UUID userId, @Param("spaceId") UUID spaceId);

        // Fetches answer counts for multiple posts in ONE query
        @Query("SELECT a.postId, COUNT(a) FROM Answer a WHERE a.postId IN :postIds GROUP BY a.postId")
        java.util.List<Object[]> countAnswersByPostIds(@Param("postIds") java.util.List<UUID> postIds);
}