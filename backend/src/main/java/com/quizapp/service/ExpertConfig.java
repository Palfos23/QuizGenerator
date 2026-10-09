package com.quizapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The daily quiz's "expert": the one account (the question author) whose scores are shown apart from the
 * normal leaderboards - as a "beat the expert" benchmark - instead of ranking among the players or being
 * able to win the weekly competition. Set with quiz.expert.user-id (env EXPERT_USER_ID in production);
 * 0 or unset means there is no expert and every account is an ordinary player.
 */
@Component
public class ExpertConfig {

    private volatile long expertUserId;

    public ExpertConfig(@Value("${quiz.expert.user-id:0}") long expertUserId) {
        this.expertUserId = expertUserId;
    }

    public boolean isExpert(Long userId) {
        return userId != null && expertUserId > 0 && userId == expertUserId;
    }

    public long getExpertUserId() {
        return expertUserId;
    }

    // Tests only - the shared test database hands out user ids in creation order, so a test that needs a
    // known expert points this at one it just created and puts it back afterwards.
    void setExpertUserId(long expertUserId) {
        this.expertUserId = expertUserId;
    }
}
