package com.quizapp.service;

import com.quizapp.dto.DailyQuizSubmitRequest;
import com.quizapp.dto.DailyQuizWeeklyDto;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.model.AppUser;
import com.quizapp.model.DailyQuizAnswer;
import com.quizapp.model.DailyQuizAnswerVerdict;
import com.quizapp.model.DailyQuizAttempt;
import com.quizapp.model.DailyQuizAttemptStatus;
import com.quizapp.model.DailyQuizResult;
import com.quizapp.model.DailyQuizSet;
import com.quizapp.model.Language;
import com.quizapp.model.Question;
import com.quizapp.repository.AppUserRepository;
import com.quizapp.repository.DailyQuizAnswerRepository;
import com.quizapp.repository.DailyQuizAttemptRepository;
import com.quizapp.repository.DailyQuizResultRepository;
import com.quizapp.repository.DailyQuizSetRepository;
import com.quizapp.repository.QuestionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The weekly winner (Monday-Sunday, graded daily scores added up) and the 7-day removal of old quizzes.
// The two are linked: a quiz is deleted before some weeks have even finished, so the weekly standings are
// worked out from small per-player-per-day result records that are kept after the quiz is gone.
//
// Builds and removes its own data - the other daily quiz tests share this database.
@SpringBootTest
class DailyQuizWeeklyTest {

    @Autowired
    private DailyQuizService dailyQuizService;
    @Autowired
    private DailyQuizReviewService reviewService;
    @Autowired
    private DailyQuizWeeklyService weeklyService;
    @Autowired
    private DailyQuizResultService resultService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private DailyQuizSetRepository setRepository;
    @Autowired
    private DailyQuizAttemptRepository attemptRepository;
    @Autowired
    private DailyQuizAnswerRepository answerRepository;
    @Autowired
    private DailyQuizResultRepository resultRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private ExpertConfig expertConfig;

    // Fixed, far-future "today" for the standings tests so they can't touch real data or each other.
    private static final LocalDate TODAY = LocalDate.of(2031, 3, 5); // a Wednesday
    private static final AtomicInteger DAY_COUNTER = new AtomicInteger();

    private final List<AppUser> users = new ArrayList<>();
    private final List<Question> questions = new ArrayList<>();
    private final List<DailyQuizSet> sets = new ArrayList<>();

    private long expertBefore;

    @org.junit.jupiter.api.BeforeEach
    void rememberExpert() {
        expertBefore = expertConfig.getExpertUserId();
    }

    @AfterEach
    void cleanUp() {
        expertConfig.setExpertUserId(expertBefore);
        List<Long> setIds = sets.stream().map(DailyQuizSet::getId).toList();
        if (!setIds.isEmpty()) {
            List<DailyQuizAttempt> attempts = attemptRepository.findBySet_IdIn(setIds);
            List<Long> attemptIds = attempts.stream().map(DailyQuizAttempt::getId).toList();
            if (!attemptIds.isEmpty()) answerRepository.deleteByAttempt_IdIn(attemptIds);
            attemptRepository.deleteAll(attempts);
            setRepository.deleteAll(sets.stream().filter(s -> setRepository.existsById(s.getId())).toList());
        }
        users.forEach(u -> resultRepository.deleteByUserId(u.getId()));
        questionRepository.deleteAll(questions.stream().filter(q -> questionRepository.existsById(q.getId())).toList());
        sets.clear();
        questions.clear();
        users.clear();
    }

    private AppUser newUser(String nameStem) {
        String suffix = String.valueOf(System.nanoTime());
        AppUser user = new AppUser();
        user.setEmail("weekly-" + suffix + "@example.com");
        user.setName(nameStem + " " + suffix);
        user = appUserRepository.save(user);
        users.add(user);
        return user;
    }

    private void result(AppUser user, LocalDate day, int score, boolean onLeaderboard) {
        DailyQuizResult r = new DailyQuizResult();
        r.setUserId(user.getId());
        r.setQuizDate(day);
        r.setPlayerName(user.getName());
        r.setScore(score);
        r.setMaxScore(16);
        r.setIncludeOnLeaderboard(onLeaderboard);
        resultRepository.save(r);
    }

    private Question newQuestion(String answer) {
        Question q = new Question();
        q.setQuestionText("Q " + System.nanoTime());
        q.setCategory("General");
        q.setDifficultyLevel(5);
        q.setLanguage(Language.NO);
        q.setAnswer(answer);
        q = questionRepository.save(q);
        questions.add(q);
        return q;
    }

    private DailyQuizSet newSet(LocalDate date, Question... qs) {
        DailyQuizSet set = new DailyQuizSet();
        set.setQuizDate(date);
        set.setQuestionIds(java.util.Arrays.stream(qs).map(Question::getId).toList());
        set = setRepository.save(set);
        sets.add(set);
        return set;
    }

    private LocalDate monday() {
        return DailyQuizWeeklyService.weekStart(TODAY);
    }

    private DailyQuizWeeklyDto.StandingDto standingFor(DailyQuizWeeklyDto dto, AppUser user) {
        return dto.getCurrent().getStandings().stream().filter(s -> s.getPlayerName().equals(user.getName())).findFirst().orElse(null);
    }

    // ---- the weekly standings ----

    @Test
    void aWeekIsMondayToSundayAndTheTotalIsTheSumOfTheDaysScores() {
        AppUser anna = newUser("Anna");
        AppUser bo = newUser("Bo");
        result(anna, monday().minusDays(1), 10, true); // the Sunday before - another week
        result(anna, monday(), 5, true);
        result(anna, monday().plusDays(2), 6, true);
        result(bo, monday().plusDays(1), 9, true);
        result(bo, monday().plusDays(7), 14, true); // next Monday - another week

        DailyQuizWeeklyDto dto = weeklyService.getWeekly(anna.getEmail(), TODAY);

        assertThat(dto.getCurrent().getWeekStart()).isEqualTo(monday());
        assertThat(dto.getCurrent().getWeekEnd()).isEqualTo(monday().plusDays(6));
        assertThat(standingFor(dto, anna).getTotal()).isEqualTo(11);
        assertThat(standingFor(dto, anna).getDaysPlayed()).isEqualTo(2);
        assertThat(standingFor(dto, bo).getTotal()).isEqualTo(9);
        assertThat(dto.getCurrent().getStandings().stream().map(DailyQuizWeeklyDto.StandingDto::getPlayerName).toList())
                .as("best first").containsSubsequence(anna.getName(), bo.getName());
        assertThat(standingFor(dto, anna).isYou()).isTrue();
        assertThat(standingFor(dto, bo).isYou()).isFalse();
    }

    @Test
    void aPlayerWhoOptedOutIsHiddenFromEveryoneElseButSeesThemselves() {
        AppUser visible = newUser("Visible");
        AppUser shy = newUser("Shy");
        result(visible, monday(), 5, true);
        result(shy, monday(), 15, false);

        assertThat(standingFor(weeklyService.getWeekly(visible.getEmail(), TODAY), shy)).as("others can't see them").isNull();
        assertThat(standingFor(weeklyService.getWeekly(shy.getEmail(), TODAY), shy)).as("but they see their own row").isNotNull();
    }

    // ---- the expert ("beat the expert") ----

    @Test
    void theExpertIsKeptOutOfTheStandingsAndCantWinButTheirWeekIsShownApart() {
        AppUser expert = newUser("Expert");
        AppUser anna = newUser("Anna");
        expertConfig.setExpertUserId(expert.getId());
        result(expert, monday(), 15, true);
        result(expert, monday().plusDays(1), 14, false); // even an opted-out expert still counts as the benchmark
        result(anna, monday(), 9, true);
        LocalDate lastMonday = monday().minusWeeks(1);
        result(expert, lastMonday, 16, true);
        result(anna, lastMonday.plusDays(1), 8, true);

        DailyQuizWeeklyDto dto = weeklyService.getWeekly(anna.getEmail(), TODAY);

        assertThat(standingFor(dto, expert)).as("not ranked").isNull();
        assertThat(standingFor(dto, anna).getTotal()).isEqualTo(9);
        assertThat(dto.getExpert()).isNotNull();
        assertThat(dto.getExpert().getName()).isEqualTo(expert.getName());
        assertThat(dto.getExpert().getTotal()).isEqualTo(29);
        assertThat(dto.getExpert().getDaysPlayed()).isEqualTo(2);
        // Last week's winner is the best player, not the expert who scored higher.
        assertThat(dto.getPastWeeks().get(0).getWinners()).containsExactly(anna.getName());
        assertThat(dto.getPastWeeks().get(0).getWinningScore()).isEqualTo(8);
    }

    @Test
    void withNoExpertConfiguredEveryoneIsAnOrdinaryPlayer() {
        AppUser anna = newUser("Anna");
        expertConfig.setExpertUserId(0);
        result(anna, monday(), 9, true);

        DailyQuizWeeklyDto dto = weeklyService.getWeekly(anna.getEmail(), TODAY);

        assertThat(dto.getExpert()).isNull();
        assertThat(standingFor(dto, anna)).isNotNull();
    }

    // ---- past weeks' winners ----

    @Test
    void lastWeeksWinnerIsNamedAndATieSharesTheWin() {
        AppUser anna = newUser("Anna");
        AppUser bo = newUser("Bo");
        AppUser cy = newUser("Cy");
        LocalDate lastMonday = monday().minusWeeks(1);
        result(anna, lastMonday, 7, true);
        result(anna, lastMonday.plusDays(3), 5, true); // 12
        result(bo, lastMonday.plusDays(1), 12, true);  // 12 - a tie
        result(cy, lastMonday.plusDays(2), 8, true);

        DailyQuizWeeklyDto.PastWeekDto last = weeklyService.getWeekly(cy.getEmail(), TODAY).getPastWeeks().get(0);

        assertThat(last.getWeekStart()).isEqualTo(lastMonday);
        assertThat(last.getWeekEnd()).isEqualTo(lastMonday.plusDays(6));
        assertThat(last.getWinningScore()).isEqualTo(12);
        assertThat(last.getWinners()).containsExactlyInAnyOrder(anna.getName(), bo.getName());
        assertThat(last.isProvisional()).isFalse();
    }

    @Test
    void weeksNobodyPlayedAreSkippedAndOptedOutPlayersCantWin() {
        AppUser shy = newUser("Shy");
        AppUser modest = newUser("Modest");
        result(shy, monday().minusWeeks(2), 15, false);   // would win, but opted out
        result(modest, monday().minusWeeks(2), 3, true);

        List<DailyQuizWeeklyDto.PastWeekDto> past = weeklyService.getWeekly(modest.getEmail(), TODAY).getPastWeeks();

        assertThat(past).hasSize(1); // last week and 3-4 weeks ago had no results
        assertThat(past.get(0).getWeekStart()).isEqualTo(monday().minusWeeks(2));
        assertThat(past.get(0).getWinners()).containsExactly(modest.getName());
    }

    @Test
    void aFinishedWeekIsProvisionalWhileSomeoneStillWaitsOnAnAdmin() {
        AppUser anna = newUser("Anna");
        AppUser waiting = newUser("Waiting");
        LocalDate lastMonday = monday().minusWeeks(1);
        result(anna, lastMonday, 9, true);
        Question q = newQuestion("Oslo");
        DailyQuizSet set = newSet(lastMonday.plusDays(1), q);
        DailyQuizAttempt attempt = new DailyQuizAttempt();
        attempt.setSet(set);
        attempt.setUser(waiting);
        attempt.setStatus(DailyQuizAttemptStatus.SUBMITTED);
        attemptRepository.save(attempt);

        assertThat(weeklyService.getWeekly(anna.getEmail(), TODAY).getPastWeeks().get(0).isProvisional())
                .as("an admin's decision could still change the result").isTrue();
    }

    // ---- the result records follow the attempt ----

    private void submit(DailyQuizSet set, AppUser user, String answer) {
        DailyQuizSubmitRequest request = new DailyQuizSubmitRequest();
        DailyQuizSubmitRequest.AnswerSubmission a = new DailyQuizSubmitRequest.AnswerSubmission();
        a.setQuestionId(set.getQuestionIds().get(0));
        a.setAnswerText(answer);
        request.setAnswers(List.of(a));
        dailyQuizService.submitAnswers(set.getId(), user.getEmail(), request);
    }

    @Test
    void gradingRecordsTheResultAndRegradingAndTheLeaderboardChoiceKeepItInStep() {
        AppUser user = newUser("Graded");
        Question q = newQuestion("Oslo");
        DailyQuizSet set = newSet(LocalDate.now().plusYears(50).plusDays(DAY_COUNTER.incrementAndGet()), q);

        submit(set, user, "Oslo"); // exact match -> graded at once
        DailyQuizResult recorded = resultRepository.findByUserIdAndQuizDate(user.getId(), set.getQuizDate()).orElseThrow();
        assertThat(recorded.getScore()).isEqualTo(1);
        assertThat(recorded.getMaxScore()).isEqualTo(1);
        assertThat(recorded.getPlayerName()).isEqualTo(user.getName());
        assertThat(recorded.isIncludeOnLeaderboard()).isTrue();

        dailyQuizService.setLeaderboardPreference(set.getId(), user.getEmail(), false);
        assertThat(resultRepository.findByUserIdAndQuizDate(user.getId(), set.getQuizDate()).orElseThrow().isIncludeOnLeaderboard()).isFalse();
    }

    @Test
    void anAdminChangingADecisionUpdatesTheRecordSoTheWeeklyTotalFollows() {
        AppUser user = newUser("Regraded");
        Question q = newQuestion("Oslo");
        DailyQuizSet set = newSet(LocalDate.now().plusYears(51).plusDays(DAY_COUNTER.incrementAndGet()), q);
        submit(set, user, "Oslo, i Norge"); // not exact -> waits on an admin; nothing recorded yet
        assertThat(resultRepository.findByUserIdAndQuizDate(user.getId(), set.getQuizDate())).isEmpty();

        Long answerId = reviewService.getAttemptDetail(
                attemptRepository.findBySet_IdAndUser_Email(set.getId(), user.getEmail()).orElseThrow().getId())
                .getAnswers().get(0).getAnswerId();
        reviewService.resolve(answerId, true);
        assertThat(resultRepository.findByUserIdAndQuizDate(user.getId(), set.getQuizDate()).orElseThrow().getScore()).isEqualTo(1);

        reviewService.resolve(answerId, false); // the admin changes their mind
        assertThat(resultRepository.findByUserIdAndQuizDate(user.getId(), set.getQuizDate()).orElseThrow().getScore()).isZero();
    }

    @Test
    void theStartupBackfillRecordsAttemptsGradedBeforeResultRecordsExisted() {
        AppUser user = newUser("Old");
        Question q = newQuestion("Oslo");
        DailyQuizSet set = newSet(LocalDate.now().plusYears(52).plusDays(DAY_COUNTER.incrementAndGet()), q);
        submit(set, user, "Oslo");
        resultRepository.deleteByUserId(user.getId()); // as if it was graded before the records were introduced
        assertThat(resultRepository.findByUserIdAndQuizDate(user.getId(), set.getQuizDate())).isEmpty();

        resultService.backfillFromGradedAttempts();

        assertThat(resultRepository.findByUserIdAndQuizDate(user.getId(), set.getQuizDate())).isPresent();
    }

    // ---- 7-day removal ----

    @Test
    void deletingAnOldQuizKeepsThePlayersResultSoTheirWeeklyScoreSurvives() {
        AppUser user = newUser("Survivor");
        Question q = newQuestion("Oslo");
        LocalDate today = LocalDate.now();
        DailyQuizSet set = newSet(today.minusDays(4), q);
        submit(set, user, "Oslo");

        // Time passes: the quiz reaches 7 days old and the cleanup job runs.
        set.setQuizDate(today.minusDays(7));
        setRepository.save(set);
        // the result keeps the date it was recorded under
        DailyQuizResult before = resultRepository.findByUserIdAndQuizDate(user.getId(), today.minusDays(4)).orElseThrow();
        Long attemptId = attemptRepository.findBySet_IdAndUser_Email(set.getId(), user.getEmail()).orElseThrow().getId();
        assertThat(answerRepository.findByAttempt_IdOrderByIdAsc(attemptId)).as("answers exist before the cleanup").isNotEmpty();
        dailyQuizService.deleteOldSets();

        assertThat(setRepository.findById(set.getId())).as("the quiz is gone").isEmpty();
        assertThat(attemptRepository.findBySet_Id(set.getId())).as("and its attempts").isEmpty();
        assertThat(answerRepository.findByAttempt_IdOrderByIdAsc(attemptId)).as("and every answer - the bulk of the storage").isEmpty();
        assertThat(resultRepository.findById(before.getId())).as("but the small result record stays").isPresent();
    }

    @Test
    void aQuizThatHasReachedSevenDaysIsHiddenAtOnceEvenBeforeTheCleanupRuns() {
        AppUser user = newUser("Hidden");
        Question q = newQuestion("Oslo");
        LocalDate today = LocalDate.now();
        DailyQuizSet oldest = newSet(today.minusDays(6), q);   // the oldest quiz still shown
        DailyQuizSet expired = newSet(today.minusDays(9), q);  // reached 7 days; cleanup hasn't run

        List<Long> shown = dailyQuizService.findArchive(user.getEmail()).stream().map(s -> s.getId()).toList();
        assertThat(shown).contains(oldest.getId()).doesNotContain(expired.getId());
        assertThat(dailyQuizService.getPlayState(oldest.getId(), user.getEmail())).isNotNull();
        assertThatThrownBy(() -> dailyQuizService.getPlayState(expired.getId(), user.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> reviewService.listAttemptsForSet(expired.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void anExpiredQuizsWaitingAnswersNoLongerAppearInTheAdminReviewList() {
        AppUser user = newUser("Pending");
        Question q = newQuestion("Oslo");
        DailyQuizSet expired = newSet(LocalDate.now().minusDays(9), q);
        DailyQuizAttempt attempt = new DailyQuizAttempt();
        attempt.setSet(expired);
        attempt.setUser(user);
        attempt.setStatus(DailyQuizAttemptStatus.SUBMITTED);
        attempt = attemptRepository.save(attempt);
        DailyQuizAnswer answer = new DailyQuizAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(q);
        answer.setAnswerText("Oslo, i Norge");
        answer.setVerdict(DailyQuizAnswerVerdict.PENDING);
        answer = answerRepository.save(answer);
        Long attemptId = attempt.getId();
        Long answerId = answer.getId();

        assertThat(reviewService.listPendingAttempts()).noneMatch(p -> p.getPlayerName().equals(user.getName()));
        assertThatThrownBy(() -> reviewService.getAttemptDetail(attemptId)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> reviewService.resolve(answerId, true)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resultRecordsAreAlsoPrunedButMuchLaterThanTheQuizzes() {
        AppUser user = newUser("Pruned");
        LocalDate today = LocalDate.now();
        result(user, today.minusDays(30), 5, true);  // way past the quiz's 7 days, still within the keep window
        result(user, today.minusDays(61), 5, true);  // beyond it

        resultService.pruneOld(today);

        assertThat(resultRepository.findByUserIdAndQuizDate(user.getId(), today.minusDays(30))).isPresent();
        assertThat(resultRepository.findByUserIdAndQuizDate(user.getId(), today.minusDays(61))).isEmpty();
    }

    // ---- privacy ----

    @Test
    void deletingAnAccountRemovesTheirResultRecordsAndTheExportIncludesThem() {
        AppUser user = newUser("Leaving");
        result(user, monday(), 7, true);
        result(user, monday().plusDays(1), 4, true);

        assertThat(accountService.exportData(user.getEmail()).getDailyQuizResults()).hasSize(2);

        accountService.deleteAccount(user.getEmail(), null);
        users.remove(user); // already gone

        assertThat(resultRepository.findByUserIdOrderByQuizDateDesc(user.getId())).isEmpty();
    }
}
