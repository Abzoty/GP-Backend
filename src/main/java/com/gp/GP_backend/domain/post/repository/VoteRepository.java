package com.gp.GP_backend.domain.post.repository;

import com.gp.GP_backend.domain.post.entity.TargetType;
import com.gp.GP_backend.domain.post.entity.Vote;
import com.gp.GP_backend.domain.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {


    boolean existsByTargetIdAndTargetTypeAndUserId(
        UUID targetId,
        TargetType targetType,
        UUID userId
    );
    
}