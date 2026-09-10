package com.quizapp.service;

import com.quizapp.dto.BullseyeQuestionSummaryDto;
import com.quizapp.dto.BullseyeRoundStateDto;
import com.quizapp.model.Athlete;
import com.quizapp.model.BullseyeEntry;
import com.quizapp.model.BullseyeQuestion;
import com.quizapp.repository.AthleteRepository;
import com.quizapp.repository.BullseyeQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BullseyePlayServiceTest {

    @Autowired
    private BullseyePlayService bullseyePlayService;
    @Autowired
    private BullseyeQuestionRepository bullseyeQuestionRepository;
    @Autowired
    private AthleteRepository athleteRepository;

    // Unique per test run - the H2 database in these tests is shared across
    // every test class in the same JVM run with no per-test rollback (see
    // GridPlayServiceTest-adjacent notes elsewhere), and other test classes
    // also create "Football" athletes. A live category query (entireCategoryPool)
    // would otherwise pick up every one of them, not just this test's own -
    // scoping to a nanoTime-suffixed sport name keeps each run isolated.
    private String sport;
    private BullseyeQuestion eligibleQuestion;
    private BullseyeQuestion excludedQuestion;

    @BeforeEach
    void setUp() {
        sport = "Football-" + System.nanoTime();
        Athlete a = athleteRepository.save(newAthlete("Player A " + System.nanoTime()));
        Athlete b = athleteRepository.save(newAthlete("Player B " + System.nanoTime()));

        eligibleQuestion = bullseyeQuestionRepository.save(
                newQuestion("Eligible " + System.nanoTime(), false, a, 27, b, 18));
        excludedQuestion = bullseyeQuestionRepository.save(
                newQuestion("Excluded " + System.nanoTime(), true, a, 27, b, 18));
    }

    private Athlete newAthlete(String name) {
        Athlete athlete = new Athlete();
        athlete.setName(name);
        athlete.setSport(sport);
        return athlete;
    }

    private BullseyeQuestion newQuestion(String title, boolean excluded, Athlete a1, int v1, Athlete a2, int v2) {
        BullseyeQuestion q = new BullseyeQuestion();
        q.setTitle(title);
        q.setSport(sport);
        q.setTargetValue(13);
        q.setStatLabel("goals in the Premier League 2024/25");
        q.setExcludedFromBullseye(excluded);

        BullseyeEntry e1 = new BullseyeEntry();
        e1.setAthlete(a1);
        e1.setStatValue(v1);
        e1.setOrderIndex(0);

        BullseyeEntry e2 = new BullseyeEntry();
        e2.setAthlete(a2);
        e2.setStatValue(v2);
        e2.setOrderIndex(1);

        Set<BullseyeEntry> entries = new HashSet<>();
        entries.add(e1);
        entries.add(e2);
        q.setEntries(entries);
        return q;
    }

    @Test
    void findEligibleExcludesRetiredQuestions() {
        List<BullseyeQuestionSummaryDto> eligible = bullseyePlayService.findEligible(null);
        List<Long> ids = eligible.stream().map(BullseyeQuestionSummaryDto::getId).collect(Collectors.toList());

        assertThat(ids).contains(eligibleQuestion.getId());
        assertThat(ids).doesNotContain(excludedQuestion.getId());
    }

    @Test
    void findEligibleRespectsExcludeCategories() {
        List<BullseyeQuestionSummaryDto> eligible = bullseyePlayService.findEligible(List.of(sport));

        assertThat(eligible).noneMatch(q -> q.getId().equals(eligibleQuestion.getId()));
    }

    @Test
    void battleRoundChoicesNeverReturnsExcludedOrOverCount() {
        List<BullseyeQuestionSummaryDto> choices = bullseyePlayService.getBattleRoundChoices(3, null, null);

        assertThat(choices.size()).isLessThanOrEqualTo(3);
        assertThat(choices).noneMatch(c -> c.getId().equals(excludedQuestion.getId()));
    }

    @Test
    void battleRoundChoicesRespectsExcludeIds() {
        List<BullseyeQuestionSummaryDto> choices =
                bullseyePlayService.getBattleRoundChoices(10, List.of(eligibleQuestion.getId()), null);

        assertThat(choices).noneMatch(c -> c.getId().equals(eligibleQuestion.getId()));
    }

    @Test
    void battleRoundChoicesRespectsExcludeCategories() {
        List<BullseyeQuestionSummaryDto> choices =
                bullseyePlayService.getBattleRoundChoices(10, null, List.of(sport));

        assertThat(choices).noneMatch(c -> c.getSport().equalsIgnoreCase(sport));
    }

    @Test
    void battleRoundChoicesDoesNotErrorWhenPoolIsSmallerThanCount() {
        List<BullseyeQuestionSummaryDto> choices = bullseyePlayService.getBattleRoundChoices(1000, null, null);
        assertThat(choices).isNotNull();
    }

    // The whole point of spreading choices across categories: a plain random
    // sample of `count` from a bank with only a couple of well-stocked
    // categories would routinely offer "choose one of 3 football questions"
    // instead of a real choice. With at least `count` distinct categories
    // available, none of the offered choices should repeat a category.
    @Test
    void battleRoundChoicesNeverOffersTwoQuestionsFromTheSameCategoryWhenEnoughCategoriesExist() {
        // setUp() already contributed one eligible question in `sport` - two
        // more distinct categories here makes three total for this test run,
        // enough to fill a count-of-3 pick with no repeats.
        String sportB = "Geography-" + System.nanoTime();
        String sportC = "Movies-" + System.nanoTime();
        saveEligibleQuestion(sportB);
        saveEligibleQuestion(sportC);

        List<BullseyeQuestionSummaryDto> choices = bullseyePlayService.getBattleRoundChoices(3, null, null);

        assertThat(choices).hasSize(3);
        List<String> sportsSeen = choices.stream().map(BullseyeQuestionSummaryDto::getSport).collect(Collectors.toList());
        assertThat(sportsSeen).doesNotHaveDuplicates();
    }

    @Test
    void getDistinctCategoriesIncludesAnEligibleCategoryButNotARetiredOnlyOne() {
        String retiredOnlySport = "RetiredOnly-" + System.nanoTime();
        Athlete a = athleteRepository.save(newAthleteInSport(retiredOnlySport));
        Athlete b = athleteRepository.save(newAthleteInSport(retiredOnlySport));
        bullseyeQuestionRepository.save(
                newQuestionInSport(retiredOnlySport, "Retired only " + System.nanoTime(), true, a, 1, b, 2));

        List<String> categories = bullseyePlayService.getDistinctCategories();

        assertThat(categories).contains(sport);
        assertThat(categories).doesNotContain(retiredOnlySport);
    }

    // Helper for the diversity test above - same shape as setUp()'s own
    // eligibleQuestion, just parameterized to a different category.
    private void saveEligibleQuestion(String questionSport) {
        Athlete a = athleteRepository.save(newAthleteInSport(questionSport));
        Athlete b = athleteRepository.save(newAthleteInSport(questionSport));
        bullseyeQuestionRepository.save(
                newQuestionInSport(questionSport, "Eligible " + System.nanoTime(), false, a, 27, b, 18));
    }

    private Athlete newAthleteInSport(String athleteSport) {
        Athlete athlete = new Athlete();
        athlete.setName("Player " + System.nanoTime());
        athlete.setSport(athleteSport);
        return athlete;
    }

    private BullseyeQuestion newQuestionInSport(String questionSport, String title, boolean excluded,
                                                 Athlete a1, int v1, Athlete a2, int v2) {
        BullseyeQuestion q = newQuestion(title, excluded, a1, v1, a2, v2);
        q.setSport(questionSport);
        return q;
    }

    @Test
    void multiplayerStartStateReturnsFullAnswerKey() {
        BullseyeRoundStateDto state = bullseyePlayService.getMultiplayerStartState(eligibleQuestion.getId());

        assertThat(state.getTargetValue()).isEqualTo(13);
        assertThat(state.getStatLabel()).isEqualTo("goals in the Premier League 2024/25");
        assertThat(state.getEntries()).hasSize(2);
        assertThat(state.getEntries()).anyMatch(e -> e.getStatValue().equals(27));
        assertThat(state.getEntries()).anyMatch(e -> e.getStatValue().equals(18));
    }

    @Test
    void multiplayerStartStateOnlyReturnsAuthoredEntriesWhenPoolIsNotAuto() {
        // A subject in the same category but never added as an entry - not
        // guessable, since entireCategoryPool is off for this question.
        athleteRepository.save(newAthlete("Ryan Giggs " + System.nanoTime()));

        BullseyeRoundStateDto state = bullseyePlayService.getMultiplayerStartState(eligibleQuestion.getId());

        assertThat(state.getEntries()).hasSize(2);
    }

    @Test
    void multiplayerStartStateIncludesTheWholeCategoryWhenPoolIsAuto() {
        // Not added as an entry at all - should still show up because the
        // question is in "auto pool" (entire category) mode, resolving to a
        // null stat value (0 if picked), same as Grid's entireCategoryPool.
        Athlete giggs = athleteRepository.save(newAthlete("Ryan Giggs " + System.nanoTime()));
        eligibleQuestion.setEntireCategoryPool(true);
        bullseyeQuestionRepository.save(eligibleQuestion);

        BullseyeRoundStateDto state = bullseyePlayService.getMultiplayerStartState(eligibleQuestion.getId());

        // The 2 authored entries plus Giggs, live-queried from the category.
        assertThat(state.getEntries()).hasSize(3);
        assertThat(state.getEntries()).anyMatch(e -> e.getAthleteName().equals(giggs.getName()) && e.getStatValue() == null);
        // The authored ones keep their real values regardless.
        assertThat(state.getEntries()).anyMatch(e -> e.getStatValue() != null && e.getStatValue().equals(27));
    }

    @Test
    void autoPoolNeverDuplicatesAnAthleteAlreadyListedAsAnEntry() {
        eligibleQuestion.setEntireCategoryPool(true);
        bullseyeQuestionRepository.save(eligibleQuestion);

        BullseyeRoundStateDto state = bullseyePlayService.getMultiplayerStartState(eligibleQuestion.getId());

        // Still exactly the 2 authored entries - both already exist in the
        // "Football" category, so the live query shouldn't add them again.
        assertThat(state.getEntries()).hasSize(2);
    }
}
