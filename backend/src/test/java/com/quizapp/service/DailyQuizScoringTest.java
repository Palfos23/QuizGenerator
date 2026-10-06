package com.quizapp.service;

import com.quizapp.model.Question;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// The daily Year question: spot on = 2 points, one year off = 1, anything else = 0.
class DailyQuizScoringTest {

    private Question question(String category, String answer) {
        Question q = new Question();
        q.setCategory(category);
        q.setAnswer(answer);
        return q;
    }

    @Test
    void exactYearIsTwoPoints() {
        assertThat(DailyQuizScoring.yearPoints("1994", "1994")).isEqualTo(2);
    }

    @Test
    void oneYearEitherSideIsOnePoint() {
        assertThat(DailyQuizScoring.yearPoints("1993", "1994")).isEqualTo(1);
        assertThat(DailyQuizScoring.yearPoints("1995", "1994")).isEqualTo(1);
    }

    @Test
    void twoOrMoreYearsOffIsNothing() {
        assertThat(DailyQuizScoring.yearPoints("1996", "1994")).isZero();
        assertThat(DailyQuizScoring.yearPoints("1992", "1994")).isZero();
        assertThat(DailyQuizScoring.yearPoints("1066", "1994")).isZero();
    }

    @Test
    void aYearIsFoundEvenWhenTypedAmongOtherText() {
        assertThat(DailyQuizScoring.yearPoints("I think 1994!", "1994")).isEqualTo(2);
        assertThat(DailyQuizScoring.yearPoints("  1995. ", "1994")).isEqualTo(1);
        // the stored answer may carry a note too
        assertThat(DailyQuizScoring.yearPoints("1994", "1994 (the Lillehammer Olympics)")).isEqualTo(2);
    }

    @Test
    void textWithNoYearOrAnImplausiblyLongNumberScoresNothing() {
        assertThat(DailyQuizScoring.yearPoints("banan", "1994")).isZero();
        assertThat(DailyQuizScoring.yearPoints("", "1994")).isZero();
        assertThat(DailyQuizScoring.yearPoints(null, "1994")).isZero();
        assertThat(DailyQuizScoring.yearPoints("199412", "1994")).as("not a year, just a long number").isZero();
    }

    @Test
    void aStoredAnswerThatIsNotAYearCannotBeScoredByDistance() {
        assertThat(DailyQuizScoring.yearPoints("1994", "Oslo")).isNull();
        assertThat(DailyQuizScoring.yearPoints("1994", null)).isNull();
    }

    @Test
    void onlyYearCategoryQuestionsWithAYearAnswerAreScoredByDistance() {
        assertThat(DailyQuizScoring.isScoredAsYear(question("Year", "1994"))).isTrue();
        assertThat(DailyQuizScoring.isScoredAsYear(question("  year ", "1994"))).as("case and spacing don't matter").isTrue();
        assertThat(DailyQuizScoring.isScoredAsYear(question("Year", "Oslo"))).as("no year in the answer").isFalse();
        assertThat(DailyQuizScoring.isScoredAsYear(question("History", "1994"))).as("wrong category").isFalse();
        assertThat(DailyQuizScoring.isScoredAsYear(question(null, "1994"))).isFalse();
    }

    @Test
    void maxPointsIsTwoForYearQuestionsAndOneForEverythingElse() {
        assertThat(DailyQuizScoring.maxPoints(question("Year", "1994"))).isEqualTo(2);
        assertThat(DailyQuizScoring.maxPoints(question("History", "1994"))).isEqualTo(1);
        assertThat(DailyQuizScoring.maxPoints(question("Year", "Oslo"))).isEqualTo(1);
    }
}
