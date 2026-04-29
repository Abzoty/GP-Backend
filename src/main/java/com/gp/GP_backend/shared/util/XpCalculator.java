package com.gp.GP_backend.shared.util;

/**
 * Centralised XP constants, event-type strings, and level-calculation logic
 * for the gamification system.
 *
 * <p>
 * All XP award amounts and event identifiers are defined here so that
 * tuning a reward or a reference-type string only requires a change in one
 * place. Services import these constants rather than hard-coding magic
 * numbers or raw strings.
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
    public static final int XP_ANSWER_GIVEN = 15;

    /** XP awarded to the answer author each time their answer is upvoted. */
    public static final int XP_ANSWER_UPVOTED = 5;

    /**
     * XP awarded to the post author when their post receives a Good Question vote.
     */
    public static final int XP_GOOD_QUESTION = 3;

    /** XP awarded to the answer author when their answer is accepted. */
    public static final int XP_ANSWER_ACCEPTED = 20;

    /** XP awarded when a user shares a material (file or link) in a space. */
    public static final int XP_MATERIAL_SHARED = 10;

    /** XP awarded to the material owner when another user bookmarks it. */
    public static final int XP_MATERIAL_LINKED = 3;

    /** XP awarded once per calendar day for logging in. */
    public static final int XP_DAILY_LOGIN = 2;

    /**
     * Bonus XP awarded when a streak milestone is reached
     * (7, 15, 30, or 100 consecutive days).
     */
    public static final int XP_STREAK_BONUS = 15;

    // ─── Event-type identifiers (stored in xp_transactions.event_type) ────────

    public static final String EVENT_POST_CREATED = "POST_CREATED";
    public static final String EVENT_ANSWER_GIVEN = "ANSWER_GIVEN";
    public static final String EVENT_ANSWER_UPVOTED = "ANSWER_UPVOTED";
    public static final String EVENT_GOOD_QUESTION = "GOOD_QUESTION";
    public static final String EVENT_ANSWER_ACCEPTED = "ANSWER_ACCEPTED";
    public static final String EVENT_MATERIAL_SHARED = "MATERIAL_SHARED";
    public static final String EVENT_MATERIAL_LINKED = "MATERIAL_LINKED";
    public static final String EVENT_DAILY_LOGIN = "DAILY_LOGIN";
    public static final String EVENT_STREAK_BONUS = "STREAK_BONUS";

    // ─── Reference-type discriminators (stored in xp_transactions.reference_type)

    public static final String REF_POST = "POST";
    public static final String REF_ANSWER = "ANSWER";
    public static final String REF_MATERIAL = "MATERIAL";
    public static final String REF_LOGIN = "LOGIN";
    public static final String REF_VOTE = "VOTE";
    public static final String REF_BOOKMARK = "BOOKMARK";

    // ─── Streak milestones that trigger a bonus ───────────────────────────────

    /** Consecutive-day counts at which a streak bonus is awarded. */
    public static final int[] STREAK_MILESTONES = { 7, 15, 30, 100 };

    // Utility class — no instantiation
    private XpCalculator() {
    }

    // ─── Level calculation ────────────────────────────────────────────────────

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
     * <li>Level 5: 1 600+ XP</li>
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

    /**
     * Returns {@code true} if the given streak length is a defined milestone.
     *
     * @param streakDays current consecutive-login streak.
     */
    public static boolean isStreakMilestone(int streakDays) {
        for (int milestone : STREAK_MILESTONES) {
            if (streakDays == milestone)
                return true;
        }
        return false;
    }
}