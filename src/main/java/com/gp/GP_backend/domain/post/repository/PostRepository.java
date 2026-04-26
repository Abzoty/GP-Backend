package com.gp.GP_backend.domain.post.repository;

import com.gp.GP_backend.domain.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    /** Feed sorted by newest first (default). */
    Page<Post> findBySpaceIdOrderByCreatedAtDesc(UUID spaceId, Pageable pageable);

    /** Feed sorted by most "Good Question" marks (top posts view). */
    Page<Post> findBySpaceIdOrderByGoodQuestionCountDesc(UUID spaceId, Pageable pageable);

    /**
     * Increments view counter in a single UPDATE — avoids loading the full entity.
     */
    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") UUID id);

    /** Used by US-017 (Good Question). */
    @Modifying
    @Query("UPDATE Post p SET p.goodQuestionCount = p.goodQuestionCount + 1 WHERE p.id = :id")
    void incrementGoodQuestionCount(@Param("id") UUID id);

    /** Answer count for a single post — used when building PostResponse. */
    @Query("SELECT COUNT(a) FROM Answer a WHERE a.postId = :postId")
    int countAnswersByPostId(@Param("postId") UUID postId);

    @Query("SELECT p.spaceId FROM Post p WHERE p.id = :postId")
    UUID findSpaceIdByPostId(@Param("postId") UUID postId);

    @Query("SELECT p.authorId FROM Post p WHERE p.id = :postId")
    UUID findAuthorIdByPostId(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.isSolved = true, p.acceptedAnswerId = :answerId WHERE p.id = :postId")
    void markAsSolved(@Param("postId") UUID postId, @Param("answerId") UUID answerId);

    @Query("SELECT s.name FROM Post p JOIN Space s ON p.spaceId = s.id WHERE p.id = :postId")
    String findSpaceNameByPostId(UUID postId);

    /**
     * Counts posts created by a specific user inside a specific space.
     * Used by the space leaderboard to compute per-space post stats.
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.spaceId = :spaceId AND p.authorId = :userId")
    int countBySpaceIdAndAuthorId(@Param("spaceId") UUID spaceId, @Param("userId") UUID userId);
    @Query("""
        SELECT p.authorId, COUNT(p)
        FROM Post p
        WHERE p.spaceId = :spaceId
        GROUP BY p.authorId
        """)
    List<Object[]> countBySpaceIdGroupByAuthor(@Param("spaceId") UUID spaceId);
}