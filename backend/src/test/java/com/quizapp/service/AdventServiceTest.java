package com.quizapp.service;

import com.quizapp.dto.AccountExportDto;
import com.quizapp.dto.AdventDto.*;
import com.quizapp.model.AdventDay;
import com.quizapp.model.AppUser;
import com.quizapp.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The advent calendar end to end: doors opening on their day, entries, the game master's corrections, jokers, the
// leaderboard. Everything runs in December 2031 - far from the real season - and cleans up after itself, because the
// other tests share this database.
@SpringBootTest
class AdventServiceTest {

    @Autowired private AdventService service;
    @Autowired private AdventAdminService admin;
    @Autowired private AdventDayRepository dayRepository;
    @Autowired private AdventEntryRepository entryRepository;
    @Autowired private AdventAliasRepository aliasRepository;
    @Autowired private AdventProfileRepository profileRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private AccountService accountService;
    @Autowired private AdventClock adventClock;

    private static final int YEAR = 2031;
    private final List<AppUser> users = new ArrayList<>();

    private static LocalDate d(int day) {
        return LocalDate.of(YEAR, 12, day);
    }

    @AfterEach
    void cleanUp() {
        adventClock.clear();
        List<Long> dayIds = dayRepository.findBySeasonYearOrderByDayNumberAsc(YEAR).stream().map(AdventDay::getId).toList();
        if (!dayIds.isEmpty()) {
            entryRepository.deleteByDayIdIn(dayIds);
            aliasRepository.deleteByDayIdIn(dayIds);
            dayRepository.deleteAllById(dayIds);
        }
        for (AppUser u : users) {
            profileRepository.deleteById(u.getId());
            entryRepository.deleteByUserId(u.getId());
            if (appUserRepository.existsById(u.getId())) appUserRepository.deleteById(u.getId());
        }
        users.clear();
    }

    private AppUser user(String stem) {
        String suffix = String.valueOf(System.nanoTime());
        AppUser u = new AppUser();
        u.setEmail("advent-" + suffix + "@example.com");
        u.setName(stem + " " + suffix);
        u = appUserRepository.save(u);
        users.add(u);
        return u;
    }

    private void door(int n, String type, String category, List<String> answers, Integer wildcard) {
        door(n, type, category, answers, wildcard, null);
    }

    private void door(int n, String type, String category, List<String> answers, Integer wildcard, List<String> validPool) {
        admin.saveDay(n, new AdminDayRequest(category, type, answers, wildcard, validPool), d(n));
    }

    private void normalDoor(int n) {
        door(n, "NORMAL", "Norske byer", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), null);
    }

    private DayDetailDto play(AppUser u, int n, String a, String b, String c, String e) {
        return play(u, n, false, false, a, b, c, e);
    }

    private DayDetailDto play(AppUser u, int n, boolean risky, boolean joker, String a, String b, String c, String e) {
        return service.submit(u.getEmail(), n, new EntryRequest(List.of(a, b, c, e), risky, joker), d(n));
    }

    private DayDetailDto closed(AppUser u, int n) {
        return service.dayDetail(u.getEmail(), n, d(n).plusDays(1));
    }

    // ------------------------------------------------------------------ when it is open

    @Test
    void beforeTheFirstOfDecemberItIsComingSoonAndTheDoorsAreShut() {
        AppUser u = user("Tidlig");
        StateDto state = service.state(u.getEmail(), LocalDate.of(YEAR, 11, 15));

        assertThat(state.phase()).isEqualTo("BEFORE");
        assertThat(state.daysUntilStart()).isEqualTo(16);
        assertThat(state.doors()).isEmpty();
        assertThatThrownBy(() -> service.dayDetail(u.getEmail(), 1, LocalDate.of(YEAR, 11, 15))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void thePhasesFollowTheDatesAndJanuaryStillBelongsToTheSeasonThatEnded() {
        AppUser u = user("Faser");
        assertThat(service.state(u.getEmail(), d(1)).phase()).isEqualTo("RUNNING");
        assertThat(service.state(u.getEmail(), d(24)).phase()).isEqualTo("RUNNING");
        assertThat(service.state(u.getEmail(), d(24)).todayDay()).isEqualTo(24);
        StateDto done = service.state(u.getEmail(), d(25));
        assertThat(done.phase()).isEqualTo("FINISHED");
        assertThat(done.wrappedAvailable()).isTrue();
        assertThat(service.state(u.getEmail(), LocalDate.of(YEAR + 1, 1, 10)).year()).isEqualTo(YEAR);
        assertThat(service.state(u.getEmail(), LocalDate.of(YEAR + 1, 2, 10)).phase()).isEqualTo("BEFORE");
    }

    @Test
    void aDoorOnlyOpensOnItsDayAndTodaysShowsNoAnswers() {
        normalDoor(3);
        AppUser u = user("Dag");

        assertThatThrownBy(() -> service.dayDetail(u.getEmail(), 3, d(2))).isInstanceOf(IllegalStateException.class); // not yet
        DayDetailDto open = service.dayDetail(u.getEmail(), 3, d(3));
        assertThat(open.status()).isEqualTo("OPEN");
        assertThat(open.category()).isEqualTo("Norske byer");
        assertThat(open.hidden()).isNull(); // the game master's answers stay hidden until the day is over
        assertThat(open.result()).isNull();
    }

    @Test
    void aDoorTheGamemasterNeverFilledInIsEmpty() {
        AppUser u = user("Tom");
        StateDto state = service.state(u.getEmail(), d(10));
        assertThat(state.doors().get(2).status()).isEqualTo("EMPTY");
        assertThat(state.doors().get(20).status()).isEqualTo("LOCKED");
        assertThatThrownBy(() -> service.dayDetail(u.getEmail(), 3, d(10))).hasMessageContaining("ingen luke");
    }

    @Test
    void answersAreFinalOnceTheyAreSentIn() {
        normalDoor(4);
        AppUser u = user("Endrer");
        play(u, 4, "Oslo", "Kristiansand", "Stavanger", "Drammen");

        assertThatThrownBy(() -> play(u, 4, "Oslo", "Bergen", "Stavanger", "Drammen")).hasMessageContaining("allerede sendt inn");
        assertThatThrownBy(() -> play(u, 4, false, true, "Oslo", "Kristiansand", "Stavanger", "Drammen")).hasMessageContaining("allerede sendt inn");

        DayDetailDto still = service.dayDetail(u.getEmail(), 4, d(4));
        assertThat(still.entry().answers()).containsExactly("Oslo", "Kristiansand", "Stavanger", "Drammen");
        assertThat(still.entry().jokerUsed()).isFalse();
        assertThat(entryRepository.countByDayId(dayRepository.findBySeasonYearAndDayNumber(YEAR, 4).orElseThrow().getId())).isEqualTo(1);
        // and once the day is over there is nothing to send in either
        assertThatThrownBy(() -> service.submit(u.getEmail(), 4, new EntryRequest(List.of("a", "b", "c", "e"), false, false), d(5)))
                .hasMessageContaining("stengt");
    }

    @Test
    void youMustWriteFourDifferentAnswers() {
        normalDoor(4);
        AppUser u = user("Regler");
        assertThatThrownBy(() -> play(u, 4, "Oslo", "Bergen", "Tromsø", "")).hasMessageContaining("4 svar");
        assertThatThrownBy(() -> play(u, 4, "Oslo", "oslo", "Tromsø", "Ålesund")).hasMessageContaining("samme svaret");
        assertThatThrownBy(() -> service.submit(u.getEmail(), 4, new EntryRequest(List.of("a", "b", "c"), false, false), d(4)))
                .hasMessageContaining("4 svar");
    }

    @Test
    void guestsAndAdminsWithoutAPlayerAccountCannotPlay() {
        normalDoor(4);
        assertThatThrownBy(() -> service.submit("nobody@example.com", 4, new EntryRequest(List.of("a", "b", "c", "d"), false, false), d(4)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(service.state("nobody@example.com", d(4)).player()).isFalse();
    }

    // ------------------------------------------------------------------ results and the gamemaster's corrections

    @Test
    void thePointsAreOutWhenTheDayIsOverWithTheGamemastersAnswersAndWhatEveryoneWrote() {
        normalDoor(5);
        AppUser a = user("Anna");
        AppUser b = user("Bo");
        play(a, 5, "oslo", "BERGEN", "Kristiansand", "Stavanger");
        play(b, 5, "Oslo", "Trondheim", "Narvik", "Bodø");

        DayDetailDto result = closed(a, 5);

        assertThat(result.status()).isEqualTo("CLOSED");
        assertThat(result.result().matches()).isEqualTo(2);
        assertThat(result.result().points()).isEqualTo(2);
        assertThat(result.result().answerMatches()).containsExactly(0, 1, -1, -1);
        assertThat(result.hidden()).extracting(HiddenAnswerDto::text).containsExactly("Oslo", "Bergen", "Tromsø", "Ålesund");
        assertThat(result.entrants()).isEqualTo(2);
        assertThat(result.hidden().get(0).matchedPlayers()).isEqualTo(2);
        assertThat(result.hidden().get(0).matchedPercent()).isEqualTo(100);
        assertThat(result.provisional()).as("until the game master has been through the answers").isTrue();
        // the crowd: Oslo was written by both
        assertThat(result.crowd().get(0).text()).isEqualToIgnoringCase("oslo");
        assertThat(result.crowd().get(0).count()).isEqualTo(2);
        assertThat(result.crowd().get(0).matchedIndex()).isEqualTo(0);
        assertThat(closed(b, 5).result().points()).isEqualTo(1);
    }

    @Test
    void aCorrectionChangesEveryonesPointsAtOnce() {
        normalDoor(5);
        AppUser a = user("Anna");
        AppUser b = user("Bo");
        play(a, 5, "Oslo", "Bergen by", "Kristiansand", "Stavanger");
        play(b, 5, "Oslo", "bergen by", "Narvik", "Bodø");
        assertThat(closed(a, 5).result().points()).isEqualTo(1);

        // "Bergen by" is Bergen - however it was capitalised
        admin.decide(5, new DecisionRequest("Bergen by", 1), d(5).plusDays(1));
        assertThat(closed(a, 5).result().points()).isEqualTo(2);
        assertThat(closed(b, 5).result().points()).isEqualTo(2);

        // and "Oslo" wasn't the right one after all
        admin.decide(5, new DecisionRequest("Oslo", -1), d(5).plusDays(1));
        assertThat(closed(a, 5).result().points()).isEqualTo(1);

        // taking the corrections away goes back to the automatic reading
        admin.decide(5, new DecisionRequest("Bergen by", null), d(5).plusDays(1));
        admin.decide(5, new DecisionRequest("Oslo", null), d(5).plusDays(1));
        assertThat(closed(a, 5).result().points()).isEqualTo(1);
        assertThat(closed(b, 5).result().points()).isEqualTo(1);
    }

    @Test
    void theReviewListsEachSpellingOnceWithHowItIsRead() {
        normalDoor(5);
        AppUser a = user("Anna");
        AppUser b = user("Bo");
        play(a, 5, "Oslo", "Bergen by", "Kristiansand", "Stavanger");
        play(b, 5, "OSLO", "Bergen By", "Narvik", "Bodø");

        ReviewDto review = admin.review(5, d(5).plusDays(1));

        assertThat(review.entrants()).isEqualTo(2);
        ReviewAnswerDto oslo = review.answers().stream().filter(x -> x.normalized().equals("oslo")).findFirst().orElseThrow();
        assertThat(oslo.count()).isEqualTo(2);
        assertThat(oslo.matchedIndex()).isEqualTo(0);
        ReviewAnswerDto bergenBy = review.answers().stream().filter(x -> x.normalized().equals("bergen by")).findFirst().orElseThrow();
        assertThat(bergenBy.count()).isEqualTo(2);
        assertThat(bergenBy.matchedIndex()).isEqualTo(-1);
        assertThat(bergenBy.decision()).isNull();
        assertThat(admin.approve(5, true, d(6)).approved()).isTrue();
        assertThat(closed(a, 5).provisional()).isFalse();
    }

    @Test
    void onceThereAreAnswersTheRulesOfTheDoorCannotBeChangedButTheCategoryCan() {
        normalDoor(5);
        play(user("Anna"), 5, "Oslo", "Bergen", "Tromsø", "Ålesund");

        assertThatThrownBy(() -> door(5, "NORMAL", "Norske byer", List.of("Oslo", "Bergen", "Tromsø", "Stavanger"), null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> door(5, "DOUBLE", "Norske byer", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), null))
                .isInstanceOf(IllegalStateException.class);
        door(5, "NORMAL", "Byer i Norge", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), null);
        assertThat(admin.day(5, d(5)).category()).isEqualTo("Byer i Norge");
    }

    @Test
    void aDoorIsOnlyPlayableOnceItIsComplete() {
        door(6, "NORMAL", "Halvferdig", List.of("Oslo", "Bergen"), null);
        door(7, "WILDCARD", "Uten wildcard", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), null);
        AppUser u = user("Venter");

        assertThat(service.state(u.getEmail(), d(6)).doors().get(5).status()).isEqualTo("EMPTY");
        assertThat(service.state(u.getEmail(), d(7)).doors().get(6).status()).isEqualTo("EMPTY");
        assertThat(admin.season(d(6)).days().get(5).ready()).isFalse();
    }

    @Test
    void theAdminChecksWhatTheyEnter() {
        assertThatThrownBy(() -> door(8, "NORMAL", "x", List.of("a", "b", "c", "d", "e"), null)).hasMessageContaining("4 svar");
        assertThatThrownBy(() -> door(8, "ONE_OF_FOUR", "x", List.of("a", "b"), null)).hasMessageContaining("ett svar");
        assertThatThrownBy(() -> door(8, "NORMAL", "x", List.of("Oslo", "oslo"), null)).hasMessageContaining("samme svaret");
        assertThatThrownBy(() -> door(8, "SKRIVEFEIL", "x", List.of("a"), null)).hasMessageContaining("Ukjent");
        assertThatThrownBy(() -> door(25, "NORMAL", "x", List.of("a"), null)).hasMessageContaining("Fant ikke");
        assertThatThrownBy(() -> door(8, "WILDCARD", "x", List.of("a", "b", "c", "d"), 4)).hasMessageContaining("Wildcard");
    }

    // ------------------------------------------------------------------ the kinds of door, played

    @Test
    void riskOrRewardAndTheWildcardAndTheUpsideDownDoorPlayedForReal() {
        door(10, "RISK_REWARD", "Risiko", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), null);
        door(11, "WILDCARD", "Wildcard", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), 2);
        door(12, "OPPOSITE", "Motsatt", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), null, List.of("Narvik", "Bodø", "Hamar", "Skien"));
        door(13, "ONE_OF_FOUR", "Gull", List.of("Oslo"), null);
        AppUser u = user("Spiller");

        play(u, 10, true, false, "Narvik", "Bodø", "Hamar", "Skien"); // risked and missed all
        assertThat(closed(u, 10).result().points()).isEqualTo(-1);
        play(u, 11, "Oslo", "Tromsø", "Hamar", "Skien"); // one plain match + the wildcard
        DayDetailDto wildcard = closed(u, 11);
        assertThat(wildcard.result().points()).isEqualTo(2 + 3);
        assertThat(wildcard.result().wildcardHit()).isTrue();
        assertThat(wildcard.hidden().get(2).wildcard()).isTrue();
        play(u, 12, "Narvik", "Bodø", "Hamar", "Skien");
        assertThat(closed(u, 12).result().points()).isEqualTo(5);
        play(u, 13, "Bergen", "Oslo", "Hamar", "Skien");
        assertThat(closed(u, 13).result().points()).isEqualTo(5);
    }

    @Test
    void theWildcardIsNotRevealedWhileTheDoorIsOpen() {
        door(11, "WILDCARD", "Wildcard", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), 2);
        AppUser u = user("Gjetter");
        DayDetailDto open = service.dayDetail(u.getEmail(), 11, d(11));
        assertThat(open.type()).isEqualTo("WILDCARD");
        assertThat(open.hidden()).isNull();
    }

    @Test
    void riskOnlyCountsOnARiskDoor() {
        normalDoor(14);
        AppUser u = user("Dristig");
        assertThat(play(u, 14, true, false, "Narvik", "Bodø", "Hamar", "Skien").entry().risky()).isFalse();
        assertThat(closed(u, 14).result().points()).isZero();
    }

    // ------------------------------------------------------------------ the upside-down door and nonsense answers

    private static final List<String> CITIES = List.of("Oslo", "Bergen", "Tromsø", "Ålesund");
    private static final List<String> OTHER_CITIES = List.of("Stavanger", "Bodø", "Hamar", "Skien", "Narvik");

    @Test
    void nonsenseDoesNotDodgeTheGamemasterOnTheUpsideDownDoor() {
        door(12, "OPPOSITE", "Norske byer", CITIES, null, OTHER_CITIES);
        AppUser real = user("Ekte");
        AppUser cheat = user("Jukser");
        AppUser mixed = user("Blandet");
        AppUser hit = user("Treffer");
        play(real, 12, "Stavanger", "Bodø", "Hamar", "Skien");   // all fit the category, none is the game master's: 5
        play(cheat, 12, "banan", "asdf", "elefant", "pizza");     // dodges everything - by writing nonsense: 0
        play(mixed, 12, "Stavanger", "Bodø", "Hamar", "banan");   // one answer that doesn't fit: 0
        play(hit, 12, "Oslo", "Stavanger", "Bodø", "Hamar");      // matches the game master: 0

        assertThat(closed(real, 12).result().points()).isEqualTo(5);
        assertThat(closed(real, 12).result().answerFits()).containsExactly(true, true, true, true);
        assertThat(closed(cheat, 12).result().points()).isZero();
        assertThat(closed(cheat, 12).result().matches()).as("no hits, but still nothing").isZero();
        assertThat(closed(cheat, 12).result().answerFits()).containsExactly(false, false, false, false);
        assertThat(closed(mixed, 12).result().points()).isZero();
        assertThat(closed(mixed, 12).result().answerFits()).containsExactly(true, true, true, false);
        assertThat(closed(hit, 12).result().points()).isZero();
    }

    @Test
    void theGamemasterCanApproveAnAnswerThatFitsAndEveryonesPointsFollow() {
        door(12, "OPPOSITE", "Norske byer", CITIES, null, OTHER_CITIES);
        AppUser a = user("Anna");
        AppUser b = user("Bo");
        play(a, 12, "Stavanger", "Bodø", "Hamar", "Hammerfest");
        play(b, 12, "Narvik", "Skien", "Hamar", "hammerfest");
        assertThat(closed(a, 12).result().points()).as("Hammerfest isn't on the list yet").isZero();

        ReviewDto review = admin.review(12, d(13));
        ReviewAnswerDto hammerfest = review.answers().stream().filter(x -> x.normalized().equals("hammerfest")).findFirst().orElseThrow();
        assertThat(hammerfest.fits()).isFalse();
        assertThat(hammerfest.count()).isEqualTo(2);

        admin.decide(12, new DecisionRequest("Hammerfest", AdventScoring.FITS_CATEGORY), d(13));

        assertThat(closed(a, 12).result().points()).isEqualTo(5);
        assertThat(closed(b, 12).result().points()).isEqualTo(5);
        ReviewAnswerDto after = admin.review(12, d(13)).answers().stream().filter(x -> x.normalized().equals("hammerfest")).findFirst().orElseThrow();
        assertThat(after.fits()).isTrue();
        assertThat(after.decision()).isEqualTo(AdventScoring.FITS_CATEGORY);
        assertThat(after.matchedIndex()).as("fitting is not hitting").isEqualTo(-1);

        admin.decide(12, new DecisionRequest("Hammerfest", null), d(13));
        assertThat(closed(a, 12).result().points()).isZero();
    }

    @Test
    void theListOfOtherValidAnswersCanGrowAfterPlayersHaveAnswered() {
        door(12, "OPPOSITE", "Norske byer", CITIES, null, List.of("Stavanger"));
        AppUser a = user("Anna");
        play(a, 12, "Stavanger", "Bodø", "Hamar", "Skien");
        assertThat(closed(a, 12).result().points()).isZero();

        door(12, "OPPOSITE", "Norske byer", CITIES, null, List.of("Stavanger", "Bodø", "Hamar", "Skien"));

        assertThat(closed(a, 12).result().points()).isEqualTo(5);
        assertThat(admin.day(12, d(13)).validAnswers()).hasSize(4);
    }

    @Test
    void onlyTheUpsideDownDoorHasAListOfValidAnswersAndFitsDecisions() {
        door(12, "NORMAL", "Norske byer", CITIES, null, OTHER_CITIES);
        assertThat(admin.day(12, d(12)).validAnswers()).isEmpty();
        assertThat(service.dayDetail(user("X").getEmail(), 12, d(13)).result()).isNull();
        assertThatThrownBy(() -> admin.decide(12, new DecisionRequest("Hamar", AdventScoring.FITS_CATEGORY), d(13)))
                .hasMessageContaining("opp-ned");
        assertThatThrownBy(() -> admin.decide(12, new DecisionRequest("Hamar", -3), d(13))).isInstanceOf(IllegalArgumentException.class);
        // the other doors never look at the category: a plain door still pays for nonsense that matches nothing... nothing
        AppUser u = user("Vanlig");
        play(u, 12, "banan", "asdf", "elefant", "pizza");
        assertThat(closed(u, 12).result().points()).isZero();
        assertThat(closed(u, 12).result().answerFits()).isNull();
    }

    // ------------------------------------------------------------------ jokers

    @Test
    void aJokerDoublesTheDaysPointsAndYouGetThree() {
        for (int n = 1; n <= 5; n++) normalDoor(n);
        AppUser u = user("Joker");

        play(u, 1, false, true, "Oslo", "Bergen", "Tromsø", "Skien"); // 3 -> 6
        assertThat(closed(u, 1).result().points()).isEqualTo(6);
        assertThat(closed(u, 1).result().jokerBonus()).isEqualTo(3);
        play(u, 2, false, true, "Oslo", "Bergen", "Hamar", "Skien");
        play(u, 3, false, true, "Oslo", "Hamar", "Skien", "Narvik");

        assertThat(service.dayDetail(u.getEmail(), 4, d(4)).jokersLeft()).isZero();
        assertThatThrownBy(() -> play(u, 4, false, true, "Oslo", "Bergen", "Hamar", "Skien")).hasMessageContaining("jokerne");
        play(u, 4, false, false, "Oslo", "Bergen", "Hamar", "Skien"); // without is fine
        StateDto state = service.state(u.getEmail(), d(5));
        assertThat(state.jokersLeft()).isZero();
        assertThat(state.jokersTotal()).isEqualTo(3);
    }

    @Test
    void aJokerIsSpentForGoodOnceSentInAndNeverMakesAMinusWorse() {
        door(1, "RISK_REWARD", "Risiko", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), null);
        normalDoor(2);
        AppUser u = user("Bruker");

        play(u, 2, false, true, "Oslo", "Bergen", "Tromsø", "Skien");
        assertThat(service.state(u.getEmail(), d(2)).jokersLeft()).isEqualTo(2);
        assertThatThrownBy(() -> play(u, 2, false, false, "Oslo", "Bergen", "Tromsø", "Skien")).hasMessageContaining("allerede sendt inn");
        assertThat(service.state(u.getEmail(), d(2)).jokersLeft()).as("no taking it back").isEqualTo(2);

        play(u, 1, true, true, "Narvik", "Bodø", "Hamar", "Skien"); // risk + joker, hits nothing
        assertThat(closed(u, 1).result().points()).isEqualTo(-1);
    }

    @Test
    void theFinaleIsWorthTripleAndTakesNoJoker() {
        normalDoor(24);
        AppUser u = user("Finale");

        assertThat(service.dayDetail(u.getEmail(), 24, d(24)).jokerAllowed()).isFalse();
        assertThatThrownBy(() -> play(u, 24, false, true, "Oslo", "Bergen", "Hamar", "Skien")).hasMessageContaining("finalen");
        DayDetailDto done = play(u, 24, "Oslo", "Bergen", "Hamar", "Skien");
        assertThat(done.finale()).isTrue();
        assertThat(service.dayDetail(u.getEmail(), 24, d(25)).result().points()).isEqualTo(6);
    }

    // ------------------------------------------------------------------ the spillmester, the leaderboard, streaks

    @Test
    void theGamemasterScoresWhenNobodyHits() {
        normalDoor(5);
        door(6, "DOUBLE", "X2", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), null);
        door(7, "OPPOSITE", "Motsatt", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), null);
        AppUser a = user("Anna");
        AppUser b = user("Bo");
        play(a, 5, "Narvik", "Bodø", "Hamar", "Skien");
        play(b, 5, "Kongsberg", "Mo", "Voss", "Alta");
        play(a, 6, "Narvik", "Bodø", "Hamar", "Skien");
        play(a, 7, "Oslo", "Bodø", "Hamar", "Skien");
        play(b, 7, "Oslo", "Bergen", "Hamar", "Skien");

        assertThat(closed(a, 5).gamemasterPoints()).isEqualTo(1);
        assertThat(closed(a, 6).gamemasterPoints()).isEqualTo(2);
        assertThat(closed(a, 7).gamemasterPoints()).isEqualTo(2); // Oslo and Bergen were guessed
        assertThat(service.leaderboard(a.getEmail(), d(8)).gamemasterPoints()).isEqualTo(5);
    }

    @Test
    void theLeaderboardRanksPlayersAndHidesThoseWhoOptedOutExceptFromThemselves() {
        normalDoor(5);
        AppUser a = user("Anna");
        AppUser b = user("Bo");
        AppUser c = user("Cato");
        AppUser shy = user("Sjenert");
        play(a, 5, "Oslo", "Bergen", "Tromsø", "Skien");
        play(b, 5, "Oslo", "Hamar", "Narvik", "Bodø");
        play(c, 5, "Oslo", "Hamar", "Narvik", "Bodø");
        play(shy, 5, "Oslo", "Bergen", "Tromsø", "Ålesund");
        service.setShowName(shy.getEmail(), false);

        LeaderboardDto seenByA = service.leaderboard(a.getEmail(), d(6));
        assertThat(seenByA.players()).extracting(StandingDto::name).doesNotContain(shy.getName());
        assertThat(seenByA.players().get(0).name()).isEqualTo(a.getName());
        assertThat(seenByA.players().get(0).rank()).isEqualTo(1);
        assertThat(seenByA.players().get(1).rank()).isEqualTo(2);
        assertThat(seenByA.players().get(2).rank()).as("a tie shares a rank").isEqualTo(2);
        assertThat(seenByA.yourRank()).isEqualTo(1);

        LeaderboardDto seenByShy = service.leaderboard(shy.getEmail(), d(6));
        assertThat(seenByShy.players()).anyMatch(p -> p.you() && p.name().equals(shy.getName()) && p.points() == 4);
        assertThat(seenByShy.showName()).isFalse();
    }

    @Test
    void todaysDoorDoesNotCountUntilItHasClosed() {
        normalDoor(5);
        AppUser a = user("Anna");
        play(a, 5, "Oslo", "Bergen", "Tromsø", "Ålesund");

        assertThat(service.state(a.getEmail(), d(5)).totalPoints()).isZero();
        assertThat(service.state(a.getEmail(), d(6)).totalPoints()).isEqualTo(4);
    }

    @Test
    void streaks() {
        for (int n = 1; n <= 8; n++) normalDoor(n);
        door(9, "WILDCARD", "Wildcard", List.of("Oslo", "Bergen", "Tromsø", "Ålesund"), 0);
        AppUser a = user("Streak");
        for (int n = 1; n <= 7; n++) play(a, n, "Oslo", "Bergen", "Tromsø", "Ålesund"); // 4/4 each day, seven in a row
        // skip 8, play 9

        StateDto afterSeven = service.state(a.getEmail(), d(8));
        assertThat(afterSeven.streak()).isEqualTo(7);
        assertThat(afterSeven.bestStreak()).isEqualTo(7);

        StateDto missedOne = service.state(a.getEmail(), d(9)); // door 8 closed unplayed
        assertThat(missedOne.streak()).isZero();
        assertThat(missedOne.bestStreak()).isEqualTo(7);
        play(a, 9, "Oslo", "Hamar", "Skien", "Bodø");
        assertThat(service.state(a.getEmail(), d(9)).streak()).isEqualTo(1);
    }

    @Test
    void wrappedIsOnlyThereWhenTheSeasonIsOver() {
        normalDoor(1);
        normalDoor(2);
        AppUser a = user("Ferdig");
        AppUser b = user("Konkurrent");
        play(a, 1, false, true, "Oslo", "Bergen", "Tromsø", "Skien"); // 3 -> 6 with the joker
        play(a, 2, "Oslo", "Hamar", "Skien", "Bodø");
        play(b, 1, "Oslo", "Hamar", "Skien", "Bodø");

        assertThatThrownBy(() -> service.wrapped(a.getEmail(), d(24))).isInstanceOf(IllegalStateException.class);
        WrappedDto w = service.wrapped(a.getEmail(), d(25));
        assertThat(w.totalPoints()).isEqualTo(7);
        assertThat(w.rank()).isEqualTo(1);
        assertThat(w.participants()).isEqualTo(2);
        assertThat(w.daysPlayed()).isEqualTo(2);
        assertThat(w.bestDay().day()).isEqualTo(1);
        assertThat(w.bestDay().points()).isEqualTo(6);
        assertThat(w.jokersUsed()).isEqualTo(1);
        assertThat(w.jokerBonusPoints()).isEqualTo(3);
        assertThat(w.matchesTotal()).isEqualTo(4);
        assertThat(w.syncPercent()).isEqualTo(50); // 4 of the 8 hidden answers over the two days
        assertThat(w.podium().get(0).name()).isEqualTo(a.getName());
    }

    // ------------------------------------------------------------------ your data

    @Test
    void deletingYourAccountRemovesYourAdventEntriesAndTheExportIncludesThem() {
        normalDoor(5);
        AppUser a = user("Slettes");
        AppUser b = user("Blir");
        play(a, 5, "Oslo", "Bergen", "Tromsø", "Ålesund");
        play(b, 5, "Oslo", "Hamar", "Skien", "Bodø");
        service.setShowName(a.getEmail(), false);

        AccountExportDto export = accountService.exportData(a.getEmail());
        assertThat(export.getAdventEntries()).hasSize(1);
        assertThat(export.getAdventEntries().get(0).getYear()).isEqualTo(YEAR);
        assertThat(export.getAdventEntries().get(0).getDay()).isEqualTo(5);
        assertThat(export.getAdventEntries().get(0).getAnswers()).containsExactly("Oslo", "Bergen", "Tromsø", "Ålesund");

        accountService.deleteAccount(a.getEmail(), null);

        assertThat(entryRepository.findByUserId(a.getId())).isEmpty();
        assertThat(profileRepository.findById(a.getId())).isEmpty();
        assertThat(entryRepository.findByUserId(b.getId())).hasSize(1);
    }

    // ------------------------------------------------------------------ test mode

    @Test
    void theGamemasterCanPretendItIsDecemberAndWipeTheTestDataAfterwards() {
        normalDoor(5);
        AppUser u = user("Tester");
        LocalDate november = LocalDate.of(YEAR, 11, 10); // really: before the season

        TestModeDto on = admin.setTestDate(d(6), november);
        assertThat(on.active()).isTrue();
        assertThat(on.date()).isEqualTo(d(6));
        assertThat(on.canReset()).isTrue();
        // the whole calendar now runs on the pretend date: door 5 is over, door 6 is today
        assertThat(service.state(u.getEmail()).simulatedDate()).isEqualTo(d(6));
        assertThat(service.state(u.getEmail()).phase()).isEqualTo("RUNNING");
        assertThat(service.state(u.getEmail()).todayDay()).isEqualTo(6);

        play(u, 5, "Oslo", "Bergen", "Tromsø", "Ålesund");
        admin.decide(5, new DecisionRequest("Oslo", -1), d(6));
        admin.approve(5, true, d(6));
        assertThat(entryRepository.countByDayId(dayRepository.findBySeasonYearAndDayNumber(YEAR, 5).orElseThrow().getId())).isEqualTo(1);

        admin.resetTestData(november);

        Long dayId = dayRepository.findBySeasonYearAndDayNumber(YEAR, 5).orElseThrow().getId();
        assertThat(entryRepository.countByDayId(dayId)).isZero();
        assertThat(aliasRepository.findByDayId(dayId)).isEmpty();
        assertThat(dayRepository.findBySeasonYearAndDayNumber(YEAR, 5).orElseThrow().isApproved()).isFalse();
        assertThat(dayRepository.findBySeasonYearAndDayNumber(YEAR, 5).orElseThrow().isReady()).as("the door itself stays").isTrue();

        assertThat(admin.setTestDate(null, november).active()).isFalse();
        assertThat(service.state(u.getEmail()).simulatedDate()).isNull();
    }

    @Test
    void theDataCanNeverBeWipedOnceTheSeasonHasReallyStarted() {
        normalDoor(5);
        play(user("Ekte"), 5, "Oslo", "Bergen", "Tromsø", "Ålesund");

        assertThatThrownBy(() -> admin.resetTestData(d(3))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> admin.resetTestData(d(30))).isInstanceOf(IllegalStateException.class);
        assertThat(entryRepository.countByDayId(dayRepository.findBySeasonYearAndDayNumber(YEAR, 5).orElseThrow().getId())).isEqualTo(1);
        assertThat(admin.testMode(d(3)).canReset()).isFalse();
    }

    @Test
    void thePretendDateMustBeSomewhereInTheSeason() {
        LocalDate november = LocalDate.of(YEAR, 11, 10);
        assertThatThrownBy(() -> admin.setTestDate(LocalDate.of(YEAR, 6, 1), november)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> admin.setTestDate(LocalDate.of(YEAR + 1, 3, 1), november)).isInstanceOf(IllegalArgumentException.class);
        assertThat(admin.setTestDate(LocalDate.of(YEAR + 1, 1, 15), november).active()).isTrue();
        assertThat(admin.setTestDate(LocalDate.of(YEAR, 11, 1), november).active()).isTrue();
    }
}
