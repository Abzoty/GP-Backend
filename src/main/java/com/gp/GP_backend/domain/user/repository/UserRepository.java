package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;


public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u.isActive FROM User u WHERE u.id = :userId")
    boolean isActiveById(UUID userId);

    boolean existsByStudentId(String studentId);
    
    String findNameById(UUID userId);
}
