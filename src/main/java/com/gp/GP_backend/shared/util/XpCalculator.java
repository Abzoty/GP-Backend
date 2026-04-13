package com.gp.GP_backend.shared.util;

/**
 * Centralised XP constants and level-calculation logic for the gamification
 * system.
 *
 * <p>
 * All XP award amounts are defined here so that tuning a reward only requires
 * a change in one place. Services (e.g.
 * {@link com.gp.GP_backend.domain.user.service.GamificationService})
 * import these constants rather than hard-coding magic numbers.
 *
 * <p>
 * Level thresholds follow a quadratic progression: each level requires
 * progressively more XP than the last, encouraging long-term engagement.
 */
public final class XpCalculator {

    // ─── XP awards per action ─────────────────────────────────────────────────

    /** XP awarded when a user creates a new post. */
    public static final int XP_POST_CREATED = 10;

    /** XP awarded when a user submits an answer. */
    public static final int XP_ANSWER_CREATED = 15;

    /** XP awarded to the answer author each time their answer is upvoted. */
    public static final int XP_ANSWER_UPVOTED = 5;

    /** XP awarded when a user's answer is accepted as the best answer. */
    public static final int XP_ANSWER_ACCEPTED = 25;

    /** XP awarded when a user's post receives a "Good Question" vote. */
    public static final int XP_GOOD_QUESTION = 5;

    /** XP awarded when a user shares a material in a space. */
    public static final int XP_MATERIAL_SHARED = 20;

    /** XP awarded once per day for logging in. Encourages daily engagement. */
    public static final int XP_DAILY_LOGIN = 5;

    /**
     * Bonus XP awarded when a user's current streak reaches a milestone (every 7
     * days).
     */
    public static final int XP_STREAK_BONUS = 15;

    // Utility class — no instantiation
    private XpCalculator() {
    }

    /**
     * Calculates the level for a given total XP amount.
     *
     * <p>
     * Formula: {@code level = floor(1 + sqrt(xp / 100))}
     * This gives:
     * <ul>
     * <li>Level 1: 0 – 99 XP</li>
     * <li>Level 2: 100 – 399 XP</li>
     * <li>Level 3: 400 – 899 XP</li>
     * <li>Level 5: 1600+ XP</li>
     * </ul>
     *
     * @param totalXp the user's cumulative XP (must be ≥ 0).
     * @return level as a short (minimum 1).
     */
    public static short calculateLevel(int totalXp) {
        if (totalXp <= 0)
            return 1;
        int level = (int) (1 + Math.sqrt((double) totalXp / 100));
        return (short) Math.max(1, level);
    }

    /**
     * Returns the XP threshold at which a user reaches the given level.
     *
     * <p>
     * Inverse of {@link #calculateLevel}: {@code xp = 100 * (level - 1)^2}
     *
     * @param level target level (must be ≥ 1).
     * @return minimum XP required to reach that level.
     */
    public static int xpForLevel(int level) {
        if (level <= 1)
            return 0;
        return 100 * (level - 1) * (level - 1);
    }
}
