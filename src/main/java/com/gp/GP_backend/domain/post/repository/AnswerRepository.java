package com.gp.GP_backend.domain.post.repository;

import com.gp.GP_backend.domain.post.entity.Answer;
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
}