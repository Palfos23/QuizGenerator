package com.quizapp.service;

import com.quizapp.model.Question;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Daily Quiz's scoring rules in one place. Almost every question is worth 1 point (right or
 * wrong). The one exception is the daily "Year" question - several events from one year, answered
 * with that year - which is graded by how close the guess is:
 *   spot on = 2 points, one year off = 1 point, anything else = 0.
 * It's graded automatically, never sent to an admin: there's no judgement call in "how far is
 * 1994 from 1995".
 */
public final class DailyQuizScoring {

    public static final String YEAR_CATEGORY = "Year";
    public static final int EXACT_YEAR_POINTS = 2;
    public static final int ONE_YEAR_OFF_POINTS = 1;

    // The first run of 3-4 digits not embedded in a longer number - so "1994", "in 1994", and
    // "1994." all read as 1994, while "19945" or a phone-number-ish string don't read as a year.
    private static final Pattern YEAR = Pattern.compile("(?<!\\d)(\\d{3,4})(?!\\d)");

    private DailyQuizScoring() {
    }

    public static boolean isYearQuestion(Question question) {
        return question != null && question.getCategory() != null
                && YEAR_CATEGORY.equalsIgnoreCase(question.getCategory().trim());
    }

    /**
     * True if this question is graded by year distance: it's in the Year category AND its stored
     * answer actually contains a year. (A Year question whose answer isn't a year can't be scored
     * that way, so it falls back to being an ordinary question.)
     */
    public static boolean isScoredAsYear(Question question) {
        return isYearQuestion(question) && parseYear(question.getAnswer()) != null;
    }

    /** Most points this question can earn. */
    public static int maxPoints(Question question) {
        return isScoredAsYear(question) ? EXACT_YEAR_POINTS : 1;
    }

    /** The year in a piece of text, or null if there isn't one. */
    public static Integer parseYear(String text) {
        if (text == null) return null;
        Matcher m = YEAR.matcher(text);
        return m.find() ? Integer.valueOf(m.group(1)) : null;
    }

    /**
     * Points for a year guess against the stored answer: 2 / 1 / 0, or null if the stored answer
     * isn't a year at all (a data problem - the caller should then treat it like any other
     * question rather than guess).
     */
    public static Integer yearPoints(String guess, String correctAnswer) {
        Integer correct = parseYear(correctAnswer);
        if (correct == null) return null;
        Integer guessed = parseYear(guess);
        if (guessed == null) return 0;
        int off = Math.abs(guessed - correct);
        if (off == 0) return EXACT_YEAR_POINTS;
        if (off == 1) return ONE_YEAR_OFF_POINTS;
        return 0;
    }
}
