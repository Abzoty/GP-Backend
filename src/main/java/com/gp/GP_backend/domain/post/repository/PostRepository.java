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

    /**
     * Feed sorted by newest first (default).
     * "SpaceId" has no matching scalar field anymore, but Spring Data still
     * resolves it as a nested property traversal — Post.space.id — so this
     * derived method name doesn't need to change even though {@code space}
     * is now a {@code @ManyToOne} association.
     */
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

    @Modifying
    @Query("UPDATE Post p SET p.goodQuestionCount = CASE WHEN p.goodQuestionCount > 0 THEN p.goodQuestionCount - 1 ELSE 0 END WHERE p.id = :id")
    void decrementGoodQuestionCount(@Param("id") UUID id);

    /** Answer count for a single post — used when building PostResponse. */
    @Query("SELECT COUNT(a) FROM Answer a WHERE a.post.id = :postId")
    int countAnswersByPostId(@Param("postId") UUID postId);

    @Query("SELECT p.space.id FROM Post p WHERE p.id = :postId")
    UUID findSpaceIdByPostId(@Param("postId") UUID postId);

    @Query("SELECT p.author.id FROM Post p WHERE p.id = :postId")
    UUID findAuthorIdByPostId(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE Post p SET p.isSolved = true, p.acceptedAnswerId = :answerId WHERE p.id = :postId AND p.isSolved = false")
    int markAsSolved(@Param("postId") UUID postId, @Param("answerId") UUID answerId);

    @Modifying
    @Query("UPDATE Post p SET p.isSolved = true, p.acceptedAnswerId = :answerId WHERE p.id = :postId")
    int setAcceptedAnswer(@Param("postId") UUID postId, @Param("answerId") UUID answerId);

    @Modifying
    @Query("UPDATE Post p SET p.isSolved = false, p.acceptedAnswerId = null WHERE p.id = :postId")
    int clearSolved(@Param("postId") UUID postId);

    /** Now a direct association traversal — no explicit join needed. */
    @Query("SELECT p.space.name FROM Post p WHERE p.id = :postId")
    String findSpaceNameByPostId(UUID postId);

    /**
     * Counts posts created by a specific user inside a specific space.
     * Used by the space leaderboard to compute per-space post stats.
     */
    @Query("SELECT COUNT(p) FROM Post p WHERE p.space.id = :spaceId AND p.author.id = :userId")
    int countBySpaceIdAndAuthorId(@Param("spaceId") UUID spaceId, @Param("userId") UUID userId);

    @Query("""
            SELECT p.author.id, COUNT(p)
            FROM Post p
            WHERE p.space.id = :spaceId
            GROUP BY p.author.id
            """)
    List<Object[]> countBySpaceIdGroupByAuthor(@Param("spaceId") UUID spaceId);

    /**
     * Searches posts within a space by title or body with an optional solved
     * filter. All parameters except {@code spaceId} are optional — passing
     * {@code null} skips that filter. Sorting and pagination are driven by
     * the supplied {@link Pageable}.
     */
    @Query("""
            SELECT p FROM Post p
            WHERE p.space.id = :spaceId
            AND (:query IS NULL OR p.title LIKE CONCAT('%', :query, '%')
                OR p.body LIKE CONCAT('%', :query, '%'))
            AND (:isSolved IS NULL OR p.isSolved = :isSolved)
            """)
    Page<Post> searchPosts(
            @Param("spaceId") UUID spaceId,
            @Param("query") String query,
            @Param("isSolved") Boolean isSolved,
            Pageable pageable);
}