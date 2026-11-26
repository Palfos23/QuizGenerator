package com.quizapp.model;

/** How a league season is decided - see LeagueStandings. */
public enum LeagueFormat {
    /** Like the global league: every day's daily-quiz score adds up, highest total wins. */
    POINTS,
    /** Every day you meet one other member; the higher daily score wins the matchup (3 points, draw 1). */
    HEAD_TO_HEAD
}
