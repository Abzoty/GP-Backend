package com.gp.GP_backend.domain.post.repository;

import com.gp.GP_backend.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for {@link Post} entities.
 */
public interface PostRepository extends JpaRepository<Post, UUID> {

    /** All posts in a given space, paginated. */
    Page<Post> findBySpaceId(UUID spaceId, Pageable pageable);

    /** All posts by a specific user, paginated. */
    Page<Post> findByAuthorId(UUID authorId, Pageable pageable);
}
