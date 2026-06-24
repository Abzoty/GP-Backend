package com.gp.GP_backend.domain.course.repository;

import com.gp.GP_backend.domain.course.entity.CourseRegistration;
import com.gp.GP_backend.domain.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CourseRegistration entity.
 *
 * Provides standard CRUD operations plus custom queries
 * for course lookup by user and code.
 *
 * @since 1.0
 */
@Repository
public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {

    /**
     * Finds a course registration by user and course code.
     * Useful for checking duplicates and ownership.
     *
     * @param user the user
     * @param code the course code
     * @return the registration if found, otherwise empty
     */
    Optional<CourseRegistration> findByUserAndCode(User user, String code);

    /**
     * Finds all registrations for a given user.
     *
     * @param user the user
     * @return list of all registrations for the user
     */
    List<CourseRegistration> findByUser(User user);

    /**
     * Finds all current (non-closed) registrations for a user.
     *
     * @param user the user
     * @return list of active registrations (closed = false)
     */
    @Query("SELECT cr FROM CourseRegistration cr WHERE cr.user = :user AND cr.closed = false")
    List<CourseRegistration> findCurrentByUser(@Param("user") User user);

    /**
     * Finds all closed registrations for a user.
     *
     * @param user the user
     * @return list of closed registrations (closed = true)
     */
    @Query("SELECT cr FROM CourseRegistration cr WHERE cr.user = :user AND cr.closed = true")
    List<CourseRegistration> findClosedByUser(@Param("user") User user);

    /**
     * Checks if a user has already registered for a course.
     *
     * @param user the user
     * @param code the course code
     * @return true if registration exists, false otherwise
     */
    boolean existsByUserAndCode(User user, String code);

    List<CourseRegistration> findByUserIdAndClosedFalse(UUID userId);
}