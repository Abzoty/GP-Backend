package com.gp.GP_backend.domain.user.repository;

import com.gp.GP_backend.domain.user.entity.XpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link XpTransaction} entities.
 *
 * <p>
 * XpTransaction records are <em>append-only</em> — they are never updated,
 * only created and (if ever) deleted via cascade when the owning user is
 * removed.
 */
public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {

    /** Full XP history for a user, newest first. */
    List<XpTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);
}