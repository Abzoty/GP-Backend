package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Isolated component responsible for revoking an entire refresh token family.
 *
 * <p>
 * <b>Why a separate class?</b>
 * Spring's {@code @Transactional} works by wrapping beans in a proxy at
 * startup.
 * When a method calls another method <em>on the same object</em> (e.g.
 * {@code this.foo()}),
 * the call goes directly to the real object — the proxy is bypassed, and any
 * {@code @Transactional} annotation on the called method is completely ignored.
 *
 * <p>
 * This means if {@code revokeEntireFamily} lived inside
 * {@link RefreshTokenService}
 * and was called internally, its {@code Propagation.REQUIRES_NEW} would
 * silently do
 * nothing — it would just join the caller's existing transaction instead of
 * creating
 * a new one. The family revocation would then be rolled back along with the
 * outer
 * transaction when the 401 exception was thrown, leaving the DB unchanged.
 *
 * <p>
 * By extracting this into its own {@code @Component}, Spring wraps it in its
 * own
 * proxy. When {@link RefreshTokenService} injects and calls this bean, the call
 * goes
 * through the proxy, and {@code REQUIRES_NEW} creates a genuine independent
 * transaction
 * that commits immediately — before the caller throws its exception.
 */
@Component
@RequiredArgsConstructor
class TokenFamilyRevoker {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Revokes all tokens belonging to the given family in an independent
     * transaction.
     *
     * <p>
     * {@code Propagation.REQUIRES_NEW} suspends any existing transaction, opens a
     * new one, executes the bulk UPDATE, and commits it before returning to the
     * caller.
     * The caller's transaction (and any subsequent rollback) has no effect on this
     * commit.
     *
     * @param familyId the ID of the token family to revoke.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeFamily(String familyId) {
        refreshTokenRepository.revokeAllByFamilyId(familyId);
    }
}