package com.quizapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quizapp.dto.DailyQuizScoreboardEntryDto;
import com.quizapp.dto.GridScoreboardEntryDto;
import com.quizapp.dto.LineupScoreboardEntryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// The frontend's leaderboards read a field called `isYou` to highlight the player's own row and to show
// "your rank" below the top 5. Jackson silently names a boolean `isYou()` getter "you" instead, so for a
// while the field the page looked for never existed. Uses the application's own ObjectMapper so this
// checks what is really sent over the wire.
@SpringBootTest
class ScoreboardJsonTest {

    @Autowired
    private ObjectMapper mapper;

    @Test
    void everyScoreboardEntrySendsIsYouUnderThatExactName() throws Exception {
        JsonNode daily = mapper.valueToTree(new DailyQuizScoreboardEntryDto("Anna", 3, 5, true));
        JsonNode grid = mapper.valueToTree(new GridScoreboardEntryDto("Anna", 3, 5, true, false, 0, true));
        JsonNode lineup = mapper.valueToTree(new LineupScoreboardEntryDto("Anna", 3, 5, true, true));

        for (JsonNode entry : new JsonNode[]{daily, grid, lineup}) {
            assertThat(entry.has("isYou")).as("field name the frontend reads").isTrue();
            assertThat(entry.get("isYou").asBoolean()).isTrue();
            assertThat(entry.has("you")).as("must not also leak the default name").isFalse();
        }
    }

    // Players only ever see "The Expert" - the real person behind it must never be in what's sent over the wire.
    @Test
    void theExpertIsSentWithoutAName() throws Exception {
        JsonNode day = mapper.valueToTree(new com.quizapp.dto.DailyQuizScoreboardDto.ExpertScore(14, 16));
        JsonNode week = mapper.valueToTree(new com.quizapp.dto.DailyQuizWeeklyDto.ExpertWeek(44, 3));

        assertThat(day.fieldNames()).toIterable().containsExactlyInAnyOrder("score", "maxScore");
        assertThat(week.fieldNames()).toIterable().containsExactlyInAnyOrder("total", "daysPlayed");
    }
}
