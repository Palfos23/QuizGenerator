package com.quizapp.model;

/** The kinds of door in the advent calendar. One kind per day - the rules for each live in AdventScoring. */
public enum AdventType {
    /** The plain game: a point for each of your answers that matches one of the game master's. */
    NORMAL,
    /** X2: every point doubles. */
    DOUBLE,
    /** Risk or reward: the player chooses before sending - safe (normal points) or risk (2 per match, -1 for none). */
    RISK_REWARD,
    /** The game master marks one of the four answers as the wildcard: +3 for hitting it. */
    WILDCARD,
    /** Upside down: matching the game master gives nothing, matching none of them gives 5. */
    OPPOSITE,
    /** The game master picks a single answer: hitting it gives 5 points. */
    ONE_OF_FOUR;

    /** How many hidden answers the game master writes for this kind of door. */
    public int answersRequired() {
        return this == ONE_OF_FOUR ? 1 : 4;
    }
}
