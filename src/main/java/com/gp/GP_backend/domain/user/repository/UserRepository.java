package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link User}.
 *
 * <p>
 * The generic type parameter {@code UUID} matches the new primary key type.
 * All standard CRUD methods (findById, save, delete, etc.) are inherited from
 * {@link JpaRepository} and work automatically.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Used during authentication to look up a user by their login email. */
    Optional<User> findByEmail(String email);

    /** Checked during registration to prevent duplicate email addresses. */
    boolean existsByEmail(String email);

    /** Checked during registration to prevent duplicate student IDs. */
    boolean existsByStudentId(String studentId);
}
