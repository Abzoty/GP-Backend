package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.entity.GamificationProfile;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.entity.XpTransaction;
import com.gp.GP_backend.shared.util.XpCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages XP awards, level-ups, and streak tracking for the gamification
 * system.
 *
 * <p>
 * This service is called by other services (PostService, VoteService, etc.)
 * after a gamifiable action occurs. It should NOT be called from controllers
 * directly.
 *
 * <p>
 * All methods are {@code @Transactional} and participate in the calling
 * service's transaction (propagation = REQUIRED by default), so XP changes
 * are always atomic with the action that triggered them.
 *
 * TODO: Implement the following methods:
 * -
 * {@code awardXp(User user, String eventType, int xpDelta, UUID referenceId, String referenceType)}
 * - {@code updateStreak(User user)}
 * - {@code createProfileForUser(User user)} — called in
 * UserService.registerUser()
 * - {@code getProfileByUser(User user)}
 */
@Service
@RequiredArgsConstructor
public class GamificationService {

    // TODO: inject GamificationProfileRepository and XpTransactionRepository once
    // created

    /**
     * Awards XP to a user for a given action and recalculates their level.
     *
     * <p>
     * Steps:
     * <ol>
     * <li>Load (or create) the user's {@link GamificationProfile}.</li>
     * <li>Add {@code xpDelta} to {@code xpPoints}.</li>
     * <li>Recalculate level via {@link XpCalculator#calculateLevel}.</li>
     * <li>Persist an immutable {@link XpTransaction} audit record.</li>
     * <li>Save the updated profile.</li>
     * </ol>
     */
    @Transactional
    public void awardXp(User user, String eventType, int xpDelta) {
        // TODO: implement
        throw new UnsupportedOperationException("GamificationService.awardXp not yet implemented");
    }
}
