package com.gp.GP_backend.domain.post.repository;

import com.gp.GP_backend.domain.post.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link Answer} entities.
 */
public interface AnswerRepository extends JpaRepository<Answer, UUID> {

    /** All answers for a post, sorted by upvotes descending (best first). */
    List<Answer> findByPostIdOrderByUpvoteCountDesc(UUID postId);
}
