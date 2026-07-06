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


@Repository
public interface CourseRegistrationRepository extends JpaRepository<CourseRegistration, Long> {

    Optional<CourseRegistration> findByUserAndCode(User user, String code);

    List<CourseRegistration> findByUser(User user);

    @Query("SELECT cr FROM CourseRegistration cr WHERE cr.user = :user AND cr.closed = false")
    List<CourseRegistration> findCurrentByUser(@Param("user") User user);

    @Query("SELECT cr FROM CourseRegistration cr WHERE cr.user = :user AND cr.closed = true")
    List<CourseRegistration> findClosedByUser(@Param("user") User user);

    boolean existsByUserAndCode(User user, String code);

    List<CourseRegistration> findByUserIdAndClosedFalse(UUID userId);
}