package com.quizapp.service;

import com.quizapp.model.AdventType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// The rules of the advent calendar, one door type at a time - plain functions, no database.
class AdventScoringTest {

    private static int base(AdventType type, boolean risky, int matches, boolean wildcardHit) {
        return AdventScoring.basePoints(type, 5, risky, matches, wildcardHit);
    }

    @Test
    void aNormalDoorGivesAPointPerMatch() {
        for (int m = 0; m <= 4; m++) assertThat(base(AdventType.NORMAL, false, m, false)).isEqualTo(m);
    }

    @Test
    void anX2DoorDoublesEverything() {
        assertThat(base(AdventType.DOUBLE, false, 3, false)).isEqualTo(6);
        assertThat(base(AdventType.DOUBLE, false, 0, false)).isZero();
    }

    @Test
    void riskOrRewardPlayingSafeIsNormalPoints() {
        assertThat(base(AdventType.RISK_REWARD, false, 3, false)).isEqualTo(3);
        assertThat(base(AdventType.RISK_REWARD, false, 0, false)).isZero();
    }

    @Test
    void riskOrRewardTakingTheRiskPaysTwoPerMatchAndMinusOneForNone() {
        assertThat(base(AdventType.RISK_REWARD, true, 1, false)).isEqualTo(2);
        assertThat(base(AdventType.RISK_REWARD, true, 3, false)).isEqualTo(6);
        assertThat(base(AdventType.RISK_REWARD, true, 0, false)).isEqualTo(-1);
    }

    @Test
    void aWildcardHitIsThreeExtra() {
        assertThat(base(AdventType.WILDCARD, false, 2, false)).isEqualTo(2);
        assertThat(base(AdventType.WILDCARD, false, 2, true)).isEqualTo(5);
        assertThat(base(AdventType.WILDCARD, false, 1, true)).isEqualTo(4);
    }

    @Test
    void theUpsideDownDoorPaysFiveForMatchingNothing() {
        assertThat(base(AdventType.OPPOSITE, false, 0, false)).isEqualTo(5);
        for (int m = 1; m <= 4; m++) assertThat(base(AdventType.OPPOSITE, false, m, false)).isZero();
    }

    @Test
    void oneOfFourPaysFiveForTheGoldAnswerAndNothingOtherwise() {
        assertThat(base(AdventType.ONE_OF_FOUR, false, 1, false)).isEqualTo(5);
        assertThat(base(AdventType.ONE_OF_FOUR, false, 0, false)).isZero();
    }

    @Test
    void theFinaleTriplesWhateverTheDoorGives() {
        assertThat(AdventScoring.basePoints(AdventType.NORMAL, 24, false, 3, false)).isEqualTo(9);
        assertThat(AdventScoring.basePoints(AdventType.RISK_REWARD, 24, true, 0, false)).isEqualTo(-3);
        assertThat(AdventScoring.basePoints(AdventType.NORMAL, 23, false, 3, false)).isEqualTo(3);
    }

    @Test
    void theJokerDoublesGainsButNeverMakesAMinusWorse() {
        assertThat(AdventScoring.withJoker(3, true)).isEqualTo(6);
        assertThat(AdventScoring.withJoker(3, false)).isEqualTo(3);
        assertThat(AdventScoring.withJoker(0, true)).isZero();
        assertThat(AdventScoring.withJoker(-1, true)).isEqualTo(-1);
    }

    @Test
    void theGamemasterScoresWhenNobodyHitsAnything() {
        int[] nobody = {0, 0, 0, 0};
        int[] someone = {0, 2, 0, 0};
        assertThat(AdventScoring.gamemasterPoints(AdventType.NORMAL, 5, 10, false, nobody)).isEqualTo(1);
        assertThat(AdventScoring.gamemasterPoints(AdventType.NORMAL, 5, 10, true, someone)).isZero();
        assertThat(AdventScoring.gamemasterPoints(AdventType.DOUBLE, 5, 10, false, nobody)).isEqualTo(2);
        assertThat(AdventScoring.gamemasterPoints(AdventType.WILDCARD, 5, 10, false, nobody)).isEqualTo(1);
        assertThat(AdventScoring.gamemasterPoints(AdventType.ONE_OF_FOUR, 5, 10, false, new int[] {0})).isEqualTo(1);
        assertThat(AdventScoring.gamemasterPoints(AdventType.NORMAL, 24, 10, false, nobody)).isEqualTo(3);
        assertThat(AdventScoring.gamemasterPoints(AdventType.NORMAL, 5, 0, false, nobody)).as("no players, no points").isZero();
    }

    @Test
    void onTheUpsideDownDoorTheGamemasterScoresForEveryAnswerSomebodyGuessed() {
        assertThat(AdventScoring.gamemasterPoints(AdventType.OPPOSITE, 5, 10, true, new int[] {3, 0, 1, 0})).isEqualTo(2);
        assertThat(AdventScoring.gamemasterPoints(AdventType.OPPOSITE, 5, 10, true, new int[] {1, 1, 1, 1})).isEqualTo(4);
        assertThat(AdventScoring.gamemasterPoints(AdventType.OPPOSITE, 5, 10, false, new int[] {0, 0, 0, 0})).isZero();
    }

    @Test
    void answersAreComparedIgnoringCaseAccentsAndPunctuation() {
        assertThat(AdventScoring.normalize("  Ålesund!! ")).isEqualTo("alesund");
        assertThat(AdventScoring.normalize("TRØNDELAG")).isEqualTo("trondelag");
        assertThat(AdventScoring.normalize("Æ")).isEqualTo("ae");
        assertThat(AdventScoring.normalize("Sørlandet, Norge")).isEqualTo("sorlandet norge");
        assertThat(AdventScoring.normalize(null)).isEmpty();
    }

    @Test
    void theGamemastersCorrectionsBeatTheAutomaticReading() {
        List<String> hidden = List.of("Oslo", "Bergen", "Tromsø", "Ålesund");
        assertThat(AdventScoring.matchIndex("tromso", hidden, Map.of())).isEqualTo(2);
        assertThat(AdventScoring.matchIndex("Bergen by", hidden, Map.of())).isEqualTo(-1);
        assertThat(AdventScoring.matchIndex("Bergen by", hidden, Map.of("bergen by", 1))).isEqualTo(1);
        assertThat(AdventScoring.matchIndex("Oslo", hidden, Map.of("oslo", -1))).as("a match taken away").isEqualTo(-1);
        assertThat(AdventScoring.matchIndex("", hidden, Map.of())).isEqualTo(-1);
    }
}
