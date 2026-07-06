package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.XpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {

    List<XpTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByEventKey(String eventKey);
    
    Optional<XpTransaction> findByEventKey(String eventKey);
}