package com.quizapp.model;

/** Who or what decided a daily quiz answer: how it was graded. Null on answers graded before this was recorded. */
public enum DailyQuizDecisionSource {
    /** The same text as the stored answer. */
    EXACT,
    /** The same once case, accents and punctuation are ignored. */
    NORMALIZED,
    /** A decision the admin asked to remember for this question. */
    RULE,
    /** Decided by an admin in the review. */
    ADMIN,
    /** Decided by an admin because the player complained. */
    COMPLAINT
}
