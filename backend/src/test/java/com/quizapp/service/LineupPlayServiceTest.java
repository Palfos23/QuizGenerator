package com.quizapp.service;

import com.quizapp.dto.LineupGuessResultDto;
import com.quizapp.dto.LineupPlayStateDto;
import com.quizapp.dto.LineupScoreboardDto;
import com.quizapp.dto.LineupSummaryDto;
import com.quizapp.model.AppUser;
import com.quizapp.model.Athlete;
import com.quizapp.model.Lineup;
import com.quizapp.model.LineupEntry;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.LineupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Exercises the new LineupAttempt-backed persistence end to end through the
// service layer - the same thing GridPlayService already has working, now
// verified for Lineup: a solo attempt is created lazily, guesses/strikes
// persist across calls, giving up marks completed+revealed, and the
// scoreboard reflects completed attempts (respecting the opt-out).
@SpringBootTest
class LineupPlayServiceTest {

    @Autowired
    private LineupPlayService lineupPlayService;
    @Autowired
    private LineupRepository lineupRepository;
    @Autowired
    private AthleteRepository athleteRepository;
    @Autowired
    private AppUserRepository appUserRepository;

    private Lineup lineup;
    private Athlete correctPlayer;
    private Athlete gkPlayer;
    private Athlete decoyPlayer;
    private String userEmail;

    @BeforeEach
    void setUp() {
        correctPlayer = athleteRepository.save(newAthlete("Bukayo Saka"));
        gkPlayer = athleteRepository.save(newAthlete("David Raya"));
        Athlete gk = gkPlayer;
        decoyPlayer = athleteRepository.save(newAthlete("Some Decoy"));

        Lineup l = new Lineup();
        l.setTitle("Test Board " + System.nanoTime());
        l.setTeamName("Arsenal");
        l.setOpponentName("Chelsea");
        l.setWeekStartDate(LocalDate.now().minusDays(1));
        l.setFormation("4-3-3");
        l.setMaxStrikes(3);

        LineupEntry gkEntry = new LineupEntry();
        gkEntry.setAthlete(gk);
        gkEntry.setSlotIndex(0);
        gkEntry.setShirtNumber(1);

        LineupEntry outfieldEntry = new LineupEntry();
        outfieldEntry.setAthlete(correctPlayer);
        outfieldEntry.setSlotIndex(1);
        outfieldEntry.setShirtNumber(7);

        Set<LineupEntry> entries = new HashSet<>();
        entries.add(gkEntry);
        entries.add(outfieldEntry);
        l.setEntries(entries);

        lineup = lineupRepository.save(l);

        userEmail = "player-" + System.nanoTime() + "@example.com";
        AppUser user = new AppUser();
        user.setEmail(userEmail);
        user.setName("Test Player");
        appUserRepository.save(user);
    }

    private Athlete newAthlete(String name) {
        Athlete a = new Athlete();
        a.setName(name);
        a.setSport(Lineup.CATEGORY);
        return a;
    }

    @Test
    void freshAttemptStartsWithNothingSolved() {
        LineupPlayStateDto state = lineupPlayService.getPlayState(lineup.getId(), userEmail);
        assertThat(state.getStrikesUsed()).isZero();
        assertThat(state.isCompleted()).isFalse();
        assertThat(state.getSlots()).allMatch(s -> !s.isSolved());
    }

    @Test
    void correctGuessPersistsAndShowsUpOnReload() {
        LineupGuessResultDto result = lineupPlayService.guess(lineup.getId(), userEmail, correctPlayer.getId());
        assertThat(result.isCorrect()).isTrue();
        assertThat(result.getSlot().getAthleteName()).isEqualTo("Bukayo Saka");
        assertThat(result.isAllSolved()).isFalse(); // GK slot still open

        // A brand new call re-fetches the same persisted attempt rather than starting over.
        LineupPlayStateDto reloaded = lineupPlayService.getPlayState(lineup.getId(), userEmail);
        assertThat(reloaded.getSlots()).anyMatch(s -> s.isSolved() && s.isGuessedByUser()
                && "Bukayo Saka".equals(s.getAthleteName()));
    }

    @Test
    void wrongGuessIncrementsStrikesAndCompletesAtMax() {
        // maxStrikes = 3 on this board
        lineupPlayService.guess(lineup.getId(), userEmail, decoyPlayer.getId());
        lineupPlayService.guess(lineup.getId(), userEmail, decoyPlayer.getId());
        LineupGuessResultDto third = lineupPlayService.guess(lineup.getId(), userEmail, decoyPlayer.getId());

        assertThat(third.isCorrect()).isFalse();
        assertThat(third.getStrikesUsed()).isEqualTo(3);
        assertThat(third.isGameOver()).isTrue();

        assertThatThrownBy(() -> lineupPlayService.guess(lineup.getId(), userEmail, correctPlayer.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void revealMarksCompletedAndFillsInEveryName() {
        lineupPlayService.guess(lineup.getId(), userEmail, correctPlayer.getId());
        LineupPlayStateDto revealed = lineupPlayService.reveal(lineup.getId(), userEmail);

        assertThat(revealed.isRevealed()).isTrue();
        assertThat(revealed.isCompleted()).isTrue();
        assertThat(revealed.getSlots()).allMatch(s -> s.isSolved() && s.getAthleteName() != null);
        // The GK slot was revealed, not guessed - shouldn't count toward "found".
        assertThat(revealed.getSlots()).anyMatch(s -> s.isSolved() && !s.isGuessedByUser());
    }

    @Test
    void scoreboardReflectsOnlyCompletedAttemptsAndRespectsOptOut() {
        // This user completes the board (2 correct guesses out of 2 slots).
        lineupPlayService.guess(lineup.getId(), userEmail, correctPlayer.getId());
        lineupPlayService.guess(lineup.getId(), userEmail, gkPlayer.getId());

        LineupScoreboardDto board = lineupPlayService.getScoreboard(lineup.getId(), userEmail);
        assertThat(board.getEntries()).hasSize(1);
        assertThat(board.getEntries().get(0).getGuessedCount()).isEqualTo(2);
        assertThat(board.getEntries().get(0).isYou()).isTrue();
        assertThat(board.getYourLeaderboardPreference()).isTrue();

        lineupPlayService.setLeaderboardPreference(lineup.getId(), userEmail, false);
        String otherEmail = "watcher-" + System.nanoTime() + "@example.com";
        LineupScoreboardDto asOtherViewer = lineupPlayService.getScoreboard(lineup.getId(), otherEmail);
        // Opted out and not you - hidden from the visible list, but still counted in the average.
        assertThat(asOtherViewer.getEntries()).isEmpty();
        assertThat(asOtherViewer.getAverageScore()).isEqualTo(2.0);
    }

    @Test
    void listShowsStatusAndGuessedCountPerBoard() {
        List<LineupSummaryDto> before = lineupPlayService.findAll(userEmail);
        LineupSummaryDto beforeDto = before.stream().filter(d -> d.getId().equals(lineup.getId())).findFirst().orElseThrow();
        assertThat(beforeDto.getStatus()).isEqualTo("NOT_STARTED");
        assertThat(beforeDto.getEntryCount()).isEqualTo(2);

        lineupPlayService.guess(lineup.getId(), userEmail, correctPlayer.getId());
        List<LineupSummaryDto> after = lineupPlayService.findAll(userEmail);
        LineupSummaryDto afterDto = after.stream().filter(d -> d.getId().equals(lineup.getId())).findFirst().orElseThrow();
        assertThat(afterDto.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(afterDto.getGuessedCount()).isEqualTo(1);
    }
}
