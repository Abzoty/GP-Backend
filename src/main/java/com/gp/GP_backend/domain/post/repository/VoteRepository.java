package com.gp.GP_backend.domain.post.repository;

import com.gp.GP_backend.domain.post.entity.TargetType;
import com.gp.GP_backend.domain.post.entity.Vote;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, UUID> {

        boolean existsByTargetIdAndTargetTypeAndUserId(
                        UUID targetId,
                        TargetType targetType,
                        UUID userId);

        Optional<Vote> findByTargetIdAndTargetTypeAndUserId(
                        UUID targetId,
                        TargetType targetType,
                        UUID userId);

        @Modifying
        @Transactional
        @Query("DELETE FROM Vote v WHERE v.targetId = :targetId AND v.targetType = :targetType")
        void deleteByTargetIdAndTargetType(
                        @Param("targetId") UUID targetId,
                        @Param("targetType") TargetType targetType);

        /**
         * Deletes all votes cast on any answer that belongs to the given post.
         * Used during post deletion to clean up answer-level votes before the
         * answers themselves are removed.
         *
         * NOTE: {@code targetId} on Vote stays a bare UUID — it's a polymorphic
         * "generic FK" that can point at either a Post or an Answer depending on
         * {@code targetType}, so it can't be modeled as a single typed
         * association.
         */
        @Transactional
        @Modifying
        @Query("""
                        DELETE FROM Vote v
                        WHERE v.targetType = 'ANSWER'
                        AND EXISTS (
                            SELECT 1 FROM Answer a
                            WHERE a.id = v.targetId
                            AND a.post.id = :postId
                        )
                        """)
        void deleteVotesByPostAnswers(UUID postId);
}