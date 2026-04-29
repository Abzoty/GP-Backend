package com.gp.GP_backend.domain.user.service;

import com.gp.GP_backend.domain.user.entity.GamificationProfile;
import com.gp.GP_backend.domain.user.entity.User;
import com.gp.GP_backend.domain.user.repository.GamificationProfileRepository;
import com.gp.GP_backend.domain.user.repository.UserRepository;
import com.gp.GP_backend.domain.user.repository.XpTransactionRepository;
import com.gp.GP_backend.shared.util.XpCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GamificationServiceIntegrationTest {

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GamificationProfileRepository gamificationProfileRepository;

    @Autowired
    private XpTransactionRepository xpTransactionRepository;

    @Test
    void awardXpShouldBeIdempotentForSameReference() {
        User user = userRepository.save(newUser("idempotent"));
        UUID referenceId = UUID.randomUUID();

        gamificationService.awardXp(
                user.getId(),
                XpCalculator.EVENT_POST_CREATED,
                XpCalculator.XP_POST_CREATED,
                referenceId,
                XpCalculator.REF_POST);
        gamificationService.awardXp(
                user.getId(),
                XpCalculator.EVENT_POST_CREATED,
                XpCalculator.XP_POST_CREATED,
                referenceId,
                XpCalculator.REF_POST);

        GamificationProfile profile = gamificationProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertEquals(XpCalculator.XP_POST_CREATED, profile.getXpPoints());
        assertEquals(1, profile.getTotalPosts());
        assertEquals(1, xpTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());
    }

    @Test
    void awardXpShouldAwardTwiceForDifferentReferences() {
        User user = userRepository.save(newUser("two-posts"));

        UUID post1 = UUID.randomUUID();
        UUID post2 = UUID.randomUUID();

        gamificationService.awardXp(
                user.getId(),
                XpCalculator.EVENT_POST_CREATED,
                XpCalculator.XP_POST_CREATED,
                post1,
                XpCalculator.REF_POST);
        gamificationService.awardXp(
                user.getId(),
                XpCalculator.EVENT_POST_CREATED,
                XpCalculator.XP_POST_CREATED,
                post2,
                XpCalculator.REF_POST);

        GamificationProfile profile = gamificationProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertEquals(XpCalculator.XP_POST_CREATED * 2, profile.getXpPoints());
        assertEquals(2, profile.getTotalPosts());
        assertEquals(2, xpTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());
    }

    @Test
    void trackDailyLoginShouldOnlyAwardOncePerDay() {
        User user = userRepository.save(newUser("daily"));

        gamificationService.trackDailyLogin(user.getId());
        gamificationService.trackDailyLogin(user.getId());

        GamificationProfile profile = gamificationProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertEquals(XpCalculator.XP_DAILY_LOGIN, profile.getXpPoints());
        assertEquals((short) 1, profile.getCurrentStreakDays().shortValue());

        long dailyLoginCount = xpTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .filter(tx -> XpCalculator.EVENT_DAILY_LOGIN.equals(tx.getEventType()))
                .count();
        assertEquals(1L, dailyLoginCount);
    }

    @Test
    void getSystemLeaderboardShouldSortByXpThenLevel() {
        User high = userRepository.save(newUser("high"));
        User mid = userRepository.save(newUser("mid"));

        gamificationService.awardXp(high.getId(), XpCalculator.EVENT_POST_CREATED, 400, UUID.randomUUID(), XpCalculator.REF_POST);
        gamificationService.awardXp(mid.getId(), XpCalculator.EVENT_POST_CREATED, 100, UUID.randomUUID(), XpCalculator.REF_POST);

        var leaderboard = gamificationService.getSystemLeaderboard(10);

        assertEquals(2, leaderboard.size());
        assertEquals(high.getId(), leaderboard.get(0).getUserId());
        assertEquals(mid.getId(), leaderboard.get(1).getUserId());
    }

    private User newUser(String suffix) {
        return User.builder()
                .email("user_" + suffix + "_" + UUID.randomUUID() + "@gp.com")
                .passwordHash("hashed-password")
                .fullName("User " + suffix)
                .studentId("SID-" + UUID.randomUUID())
                .isActive(true)
                .build();
    }
}
