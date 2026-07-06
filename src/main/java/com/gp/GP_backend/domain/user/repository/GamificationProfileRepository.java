package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.GamificationProfile;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface GamificationProfileRepository extends JpaRepository<GamificationProfile, UUID> {

        @Query("SELECT gp FROM GamificationProfile gp WHERE gp.user.id = :userId")
        Optional<GamificationProfile> findByUserId(@Param("userId") UUID userId);


        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT gp FROM GamificationProfile gp WHERE gp.user.id = :userId")
        Optional<GamificationProfile> findByUserIdForUpdate(@Param("userId") UUID userId);

        @Query("""
                        SELECT gp FROM GamificationProfile gp
                        JOIN FETCH gp.user
                        WHERE gp.user.id IN :userIds
                        """)
        List<GamificationProfile> findByUserIdsWithUser(@Param("userIds") List<UUID> userIds);

        @Query("""
                        SELECT gp FROM GamificationProfile gp
                        JOIN FETCH gp.user
                        ORDER BY gp.xpPoints DESC, gp.level DESC
                        """)
        List<GamificationProfile> findTopWithUserOrderByXpDesc(Pageable pageable);
}