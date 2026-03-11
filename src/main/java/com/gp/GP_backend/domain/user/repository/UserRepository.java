package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Data-access layer for {@link User}.
 *
 * <p>
 * Extends {@link JpaRepository} with {@code UUID} as the primary-key type,
 * giving standard CRUD operations plus pagination for free.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Used by Spring Security's {@code UserDetailsService} to load a user at login.
     */
    Optional<User> findByEmail(String email);

    /** Pre-registration duplicate check for email uniqueness. */
    boolean existsByEmail(String email);

    /** Pre-registration duplicate check for student-ID uniqueness. */
    boolean existsByStudentId(String studentId);
}