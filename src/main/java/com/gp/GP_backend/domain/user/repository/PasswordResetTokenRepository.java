package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.PasswordResetToken;
import com.gp.GP_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;


public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {


    Optional<PasswordResetToken> findByTokenHash(String tokenHash);


    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidateAllForUser(@Param("user") User user);


    void deleteByExpiryDateBeforeOrUsedTrue(Instant cutoff);
}