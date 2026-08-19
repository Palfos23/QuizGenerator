package com.quizapp.service;

import com.quizapp.dto.LineupBattleStateDto;
import com.quizapp.model.Athlete;
import com.quizapp.model.GameRoom;
import com.quizapp.model.Lineup;
import com.quizapp.model.LineupEntry;
import com.quizapp.model.RoomGameType;
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

// Mirrors GridBattleOnlineServiceTest exactly - exercises "Random" Starting XI
// Battle's new round-choice picker end to end through the service layer.
@SpringBootTest
class LineupBattleOnlineServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private LineupBattleOnlineService lineupBattleOnlineService;
    @Autowired
    private LineupRepository lineupRepository;
    @Autowired
    private AthleteRepository athleteRepository;

    private static final String HOST = "host@example.com";
    private static final String GUEST = "guest@example.com";

    @BeforeEach
    void seedEnoughBoardsToChooseFrom() {
        for (int i = 0; i < 6; i++) {
            saveLineupWithOneEntry("Battle Board " + System.nanoTime() + "-" + i);
        }
    }

    private Lineup saveLineupWithOneEntry(String title) {
        Athlete a = new Athlete();
        a.setName("Player " + System.nanoTime());
        a.setSport(Lineup.CATEGORY);
        a = athleteRepository.save(a);

        Lineup l = new Lineup();
        l.setTitle(title);
        l.setTeamName("Arsenal");
        l.setOpponentName("Chelsea");
        l.setWeekStartDate(LocalDate.now());
        l.setFormation("4-3-3");
        l.setMaxStrikes(3);

        LineupEntry e = new LineupEntry();
        e.setAthlete(a);
        e.setSlotIndex(0);
        e.setShirtNumber(9);
        Set<LineupEntry> entries = new HashSet<>();
        entries.add(e);
        l.setEntries(entries);

        return lineupRepository.save(l);
    }

    private GameRoom setUpTwoPlayerRandomRoom(int rounds) {
        GameRoom room = roomService.createRoomShell(RoomGameType.STARTING_XI_BATTLE, HOST, "Host", null);
        lineupBattleOnlineService.initializeLineupSequence(room, null, rounds);
        roomService.join(room.getRoomCode(), GUEST, "Guest", null);
        room = roomService.findByCode(room.getRoomCode());
        lineupBattleOnlineService.startGame(room, HOST);
        return room;
    }

    @Test
    void randomRoomOffersThreeChoicesToTheFirstPicker() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        LineupBattleStateDto state = lineupBattleOnlineService.getState(room, HOST);

        assertThat(state.isAwaitingLineupChoice()).isTrue();
        assertThat(state.getLineupChoices()).hasSize(3);
        assertThat(state.getTotalLineups()).isEqualTo(2);
        assertThat(state.getCurrentLineupIndex()).isZero();
        Long hostParticipantId = state.getYourParticipantId();
        assertThat(state.getPickerParticipantId()).isEqualTo(hostParticipantId);
    }

    @Test
    void repeatedPollsOfferTheSameThreeChoicesUntilPicked() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        LineupBattleStateDto first = lineupBattleOnlineService.getState(room, HOST);
        LineupBattleStateDto second = lineupBattleOnlineService.getState(room, HOST);

        List<Long> firstIds = first.getLineupChoices().stream().map(l -> l.getId()).sorted().toList();
        List<Long> secondIds = second.getLineupChoices().stream().map(l -> l.getId()).sorted().toList();
        assertThat(secondIds).isEqualTo(firstIds);
    }

    @Test
    void onlyThePickerCanChooseAndOnlyFromTheOfferedOptions() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        LineupBattleStateDto state = lineupBattleOnlineService.getState(room, HOST);
        Long offeredId = state.getLineupChoices().get(0).getId();

        assertThatThrownBy(() -> lineupBattleOnlineService.chooseLineup(room, GUEST, offeredId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not your turn");

        Lineup notOffered = lineupRepository.findAll().stream()
                .filter(l -> state.getLineupChoices().stream().noneMatch(c -> c.getId().equals(l.getId())))
                .findFirst().orElseThrow();
        assertThatThrownBy(() -> lineupBattleOnlineService.chooseLineup(room, HOST, notOffered.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("offered");
    }

    @Test
    void choosingABoardStartsTheRoundAndFinishingItOffersAFreshChoiceExcludingThePrevious() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        LineupBattleStateDto beforePick = lineupBattleOnlineService.getState(room, HOST);
        Long firstLineupId = beforePick.getLineupChoices().get(0).getId();

        LineupBattleStateDto afterPick = lineupBattleOnlineService.chooseLineup(room, HOST, firstLineupId);
        assertThat(afterPick.isAwaitingLineupChoice()).isFalse();
        assertThat(afterPick.getCurrentLineupId()).isEqualTo(firstLineupId);
        assertThat(afterPick.getSlots()).isNotEmpty();

        // The random pool is shared with other test classes' fixtures (same H2
        // instance for the whole test run), so the chosen board isn't
        // guaranteed to be this class's own single-entry ones - guess every
        // entry it actually has (turn rotates after each guess in a 2-player
        // room, so alternate who's asking), same as a real player finishing it.
        LineupBattleStateDto afterGuess = guessEveryEntry(room, firstLineupId);
        assertThat(afterGuess.isLineupComplete()).isTrue();

        LineupBattleStateDto round2 = lineupBattleOnlineService.advanceToNextLineup(room, HOST);
        assertThat(round2.isFinished()).isFalse();
        assertThat(round2.getCurrentLineupIndex()).isEqualTo(1);
        assertThat(round2.isAwaitingLineupChoice()).isTrue();
        assertThat(round2.getLineupChoices()).extracting(l -> l.getId()).doesNotContain(firstLineupId);
        Long guestParticipantId = lineupBattleOnlineService.getState(room, GUEST).getYourParticipantId();
        assertThat(round2.getPickerParticipantId()).isEqualTo(guestParticipantId);
    }

    @Test
    void gameFinishesAfterTheLastRandomRound() {
        GameRoom room = setUpTwoPlayerRandomRoom(2);
        Long hostParticipantId = lineupBattleOnlineService.getState(room, HOST).getYourParticipantId();
        Long guestParticipantId = lineupBattleOnlineService.getState(room, GUEST).getYourParticipantId();

        LineupBattleStateDto round1 = lineupBattleOnlineService.getState(room, HOST);
        assertThat(round1.getPickerParticipantId()).isEqualTo(hostParticipantId);
        Long l1 = round1.getLineupChoices().get(0).getId();
        lineupBattleOnlineService.chooseLineup(room, HOST, l1);
        guessEveryEntry(room, l1);
        LineupBattleStateDto round2 = lineupBattleOnlineService.advanceToNextLineup(room, HOST);

        assertThat(round2.isAwaitingLineupChoice()).isTrue();
        assertThat(round2.getPickerParticipantId()).isEqualTo(guestParticipantId);
        Long l2 = round2.getLineupChoices().get(0).getId();
        lineupBattleOnlineService.chooseLineup(room, GUEST, l2);
        guessEveryEntry(room, l2);

        LineupBattleStateDto finalState = lineupBattleOnlineService.advanceToNextLineup(room, HOST);
        assertThat(finalState.isFinished()).isTrue();
    }

    // The random pool is shared with other test classes' fixtures in the same
    // H2 instance, so a picked board's entry count can't be assumed - guess
    // every entry it actually has, same as a real player finishing the board.
    // The turn rotates to the other participant after every guess in this
    // 2-player room, so each guess must be submitted as whoever's actual turn
    // it currently is, not a fixed player.
    private LineupBattleStateDto guessEveryEntry(GameRoom room, Long lineupId) {
        List<Long> athleteIds = lineupRepository.findById(lineupId).orElseThrow()
                .getEntries().stream().map(e -> e.getAthlete().getId()).toList();
        Long hostParticipantId = lineupBattleOnlineService.getState(room, HOST).getYourParticipantId();
        LineupBattleStateDto last = null;
        for (Long athleteId : athleteIds) {
            Long currentTurnId = lineupBattleOnlineService.getState(room, HOST).getCurrentTurnParticipantId();
            String asEmail = currentTurnId.equals(hostParticipantId) ? HOST : GUEST;
            last = lineupBattleOnlineService.guess(room, asEmail, athleteId);
        }
        return last;
    }
}
