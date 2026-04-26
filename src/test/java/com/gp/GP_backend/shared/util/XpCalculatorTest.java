package com.gp.GP_backend.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XpCalculatorTest {

    @Test
    void calculateLevelShouldRespectBoundaryTable() {
        assertEquals(1, XpCalculator.calculateLevel(0));
        assertEquals(1, XpCalculator.calculateLevel(99));
        assertEquals(2, XpCalculator.calculateLevel(100));
        assertEquals(2, XpCalculator.calculateLevel(399));
        assertEquals(3, XpCalculator.calculateLevel(400));
    }

    @Test
    void calculateLevelShouldHandleLargeXpWithoutOverflow() {
        int largeXp = 2_000_000_000;
        short level = XpCalculator.calculateLevel(largeXp);
        assertTrue(level > 1);
    }

    @Test
    void xpForLevelShouldMatchInverseThresholds() {
        assertEquals(0, XpCalculator.xpForLevel(1));
        assertEquals(100, XpCalculator.xpForLevel(2));
        assertEquals(400, XpCalculator.xpForLevel(3));
        assertEquals(900, XpCalculator.xpForLevel(4));
    }

    @Test
    void streakMilestoneShouldMatchDefinedMilestones() {
        assertTrue(XpCalculator.isStreakMilestone(7));
        assertTrue(XpCalculator.isStreakMilestone(15));
        assertTrue(XpCalculator.isStreakMilestone(30));
        assertTrue(XpCalculator.isStreakMilestone(100));
        assertFalse(XpCalculator.isStreakMilestone(6));
        assertFalse(XpCalculator.isStreakMilestone(31));
    }
}
