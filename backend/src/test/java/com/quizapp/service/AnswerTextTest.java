package com.quizapp.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerTextTest {

    @Test
    void sameAnswerWrittenDifferentlyIsTheSameKey() {
        assertThat(AnswerText.normalize("  Ålesund!! ")).isEqualTo("alesund");
        assertThat(AnswerText.normalize("TRØNDELAG")).isEqualTo("trondelag");
        assertThat(AnswerText.normalize("Sørlandet, Norge")).isEqualTo("sorlandet norge");
        assertThat(AnswerText.key("Oslo.")).isEqualTo(AnswerText.key("oslo"));
        assertThat(AnswerText.normalize(null)).isEmpty();
    }

    @Test
    void anAnswerWithNoLettersIsStillGroupedByItsOwnText() {
        assertThat(AnswerText.normalize("???")).isEmpty();
        assertThat(AnswerText.key("???")).isEqualTo("???");
        assertThat(AnswerText.key(" ???  ")).isEqualTo("???");
    }

    @Test
    void aMisspellingIsRecognisedButADifferentAnswerIsNot() {
        assertThat(AnswerText.likelyTypo("Stavangr", "Stavanger")).isTrue();      // a letter missing
        assertThat(AnswerText.likelyTypo("Stavangerr", "Stavanger")).isTrue();    // a letter too many
        assertThat(AnswerText.likelyTypo("Stavnager", "Stavanger")).isTrue();     // two letters swapped
        assertThat(AnswerText.likelyTypo("Stavanger", "Stavanger")).as("the same is not a typo, it is correct").isFalse();
        assertThat(AnswerText.likelyTypo("Bergen", "Stavanger")).isFalse();
        assertThat(AnswerText.likelyTypo("Nina", "Nils")).as("short answers differ by a letter too easily").isFalse();
        assertThat(AnswerText.likelyTypo("Aleksander Sæther", "Alexander Sæther")).isTrue(); // long: two letters allowed
        assertThat(AnswerText.likelyTypo("", "Stavanger")).isFalse();
    }
}
