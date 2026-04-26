package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.GamificationProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link GamificationProfile} entities.
 */
public interface GamificationProfileRepository extends JpaRepository<GamificationProfile, UUID> {

        /**
         * Finds the profile for a given user, using JPQL to avoid Spring Data's
         * ambiguous property-traversal resolution for nested-association IDs.
         */
        @Query("SELECT gp FROM GamificationProfile gp WHERE gp.user.id = :userId")
        Optional<GamificationProfile> findByUserId(@Param("userId") UUID userId);

        // /**
        //  * Write-path profile lookup with row-level lock to prevent concurrent
        //  * lost updates when awarding XP.
        //  */
        // @Lock(LockModeType.PESSIMISTIC_WRITE)
        // @Query("SELECT gp FROM GamificationProfile gp WHERE gp.user.id = :userId")
        // Optional<GamificationProfile> findByUserIdForUpdate(@Param("userId") UUID userId);

        /**
         * Loads profiles for a set of user IDs in a single query, eager-fetching
         * the associated {@code User} to avoid N+1 in leaderboard mapping.
         */
        @Query("""
                        SELECT gp FROM GamificationProfile gp
                        JOIN FETCH gp.user
                        WHERE gp.user.id IN :userIds
                        """)
        List<GamificationProfile> findByUserIdsWithUser(@Param("userIds") List<UUID> userIds);

        /**
         * Top N profiles system-wide, ordered by XP descending then level descending,
         * with the associated {@code User} eager-fetched for leaderboard display.
         */
        @Query("""
                        SELECT gp FROM GamificationProfile gp
                        JOIN FETCH gp.user
                        ORDER BY gp.xpPoints DESC, gp.level DESC
                        """)
        List<GamificationProfile> findTopWithUserOrderByXpDesc(Pageable pageable);
}